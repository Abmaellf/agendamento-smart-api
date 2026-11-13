package com.agendamento.smart;

import com.agendamento.smart.dtos.AuthenticationDTO;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.dtos.user.RegisterDTO;
import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.infra.security.TokenService;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.professional.Professional;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.unit.StatusUnit;
import com.agendamento.smart.model.unit.TypeUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.ProfessionalRepository;
import com.agendamento.smart.repository.ServiceOfferingRepository;
import com.agendamento.smart.repository.UnitRepository;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.ClinicOnboardingService;
import com.agendamento.smart.service.UnitScopeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BasicUnitScopeIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String ADMIN_PASSWORD = "Administrative@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ClinicOnboardingService onboardingService;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ServiceOfferingRepository serviceRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private UnitScopeService unitScopeService;

    @Test
    void basicUserReceivesOnlyItsUnitAcrossCatalogsAndLegacyPatients() throws Exception {
        ClinicRegistrationResponse registration = onboardingService.register(new ClinicRegistrationRequest(
                new ClinicRegistrationRequest.ClinicData("Clínica escopo"),
                new ClinicRegistrationRequest.MatrixUnitData("Matriz", "America/Cuiaba"),
                new ClinicRegistrationRequest.InitialProfessionalData(
                        "Atendente", "attendant-scope@example.test", "Attendant@123"),
                new ClinicRegistrationRequest.InitialProfessionalData(
                        "Administrativo", "admin-scope@example.test", ADMIN_PASSWORD)));
        Clinic clinic = clinicRepository.findById(registration.clinicId()).orElseThrow();

        ClinicUnit branch = new ClinicUnit();
        branch.setId(UUID.randomUUID());
        branch.setClinic(clinic);
        branch.setTenantId(clinic.getId());
        branch.setName("Filial");
        branch.setTimeZone("America/Cuiaba");
        branch.setStatus(StatusUnit.ATIVO);
        branch.setType(TypeUnit.FILIAL);
        unitRepository.saveAndFlush(branch);

        User admin = userRepository.findById(registration.administrative().userId()).orElseThrow();
        registerBasicUser(registration.clinicId(), branch.getId(), admin);
        String adminToken = login("admin-scope@example.test", ADMIN_PASSWORD);
        String basicToken = login("basic-scope@example.test", "Basic@123");

        RegisterDTO unauthorizedRegistration = new RegisterDTO(
                "forbidden-scope@example.test",
                "Basic@123",
                UserRole.USER,
                branch.getId(),
                false,
                null);
        mockMvc.perform(post("/user/register/{clinicId}", registration.clinicId())
                        .header("Authorization", bearerToken(basicToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(unauthorizedRegistration)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_REGISTRATION_FORBIDDEN"));

        Patient matrixPatient = patient(clinic, unitRepository.findById(registration.unitId()).orElseThrow(), "Paciente Matriz");
        Patient branchPatient = patient(clinic, branch, "Paciente Filial");
        ServiceOffering service = service(clinic, "Consulta Escopo");
        professional(clinic, unitRepository.findById(registration.unitId()).orElseThrow(), service, "Profissional Matriz");
        professional(clinic, branch, service, "Profissional Filial");

        mockMvc.perform(get("/api/units").header("Authorization", bearerToken(basicToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(branch.getId().toString()));

        mockMvc.perform(get("/api/patients").header("Authorization", bearerToken(basicToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(branchPatient.getId().toString()));

        mockMvc.perform(get("/api/services").header("Authorization", bearerToken(basicToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(service.getId().toString()));

        mockMvc.perform(get("/api/professionals")
                        .header("Authorization", bearerToken(basicToken))
                        .param("serviceId", service.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Profissional Filial"));

        mockMvc.perform(get("/patient/list")
                        .header("Authorization", bearerToken(basicToken))
                        .param("unitId", registration.unitId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(branchPatient.getId().toString()));

        mockMvc.perform(get("/patient/list"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/auth/me").header("Authorization", bearerToken(basicToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeUnitId").value(branch.getId().toString()))
                .andExpect(jsonPath("$.unitName").value("Filial"));

        mockMvc.perform(get("/auth/list").header("Authorization", bearerToken(basicToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/auth/list"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/units").header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(get("/api/patients").header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(get("/api/professionals")
                        .header("Authorization", bearerToken(adminToken))
                        .param("serviceId", service.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(get("/auth/list").header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].password").doesNotExist());

        assertThat(matrixPatient.getUnits()).extracting(ClinicUnit::getId).containsExactly(registration.unitId());
        assertThatThrownBy(() -> unitScopeService.allowedUnits(
                User.builder().clinic(clinic).role(UserRole.USER).build()))
                .isInstanceOf(DomainException.class);

        Clinic foreignClinic = Clinic.builder().id(UUID.randomUUID()).name("Outra clínica").build();
        ClinicUnit foreignUnit = new ClinicUnit();
        foreignUnit.setId(UUID.randomUUID());
        foreignUnit.setClinic(foreignClinic);
        User crossClinicPrincipal = User.builder()
                .clinic(clinic)
                .clinicUnit(foreignUnit)
                .role(UserRole.USER)
                .build();
        assertThatThrownBy(() -> unitScopeService.allowedUnits(crossClinicPrincipal))
                .isInstanceOf(DomainException.class);
    }

    private void registerBasicUser(UUID clinicId, UUID unitId, User admin) throws Exception {
        RegisterDTO request = new RegisterDTO(
                "basic-scope@example.test",
                "Basic@123",
                UserRole.USER,
                unitId,
                false,
                null);
        mockMvc.perform(post("/user/register/{clinicId}", clinicId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk());
    }

    private String login(String login, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new AuthenticationDTO(login, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.get("token").asText();
    }

    private Patient patient(Clinic clinic, ClinicUnit unit, String name) {
        Patient patient = new Patient();
        patient.setClinic(clinic);
        patient.setName(name);
        patient.setUnits(new LinkedHashSet<>(Set.of(unit)));
        return patientRepository.saveAndFlush(patient);
    }

    private ServiceOffering service(Clinic clinic, String name) {
        ServiceOffering service = new ServiceOffering();
        service.setId(UUID.randomUUID());
        service.setClinic(clinic);
        service.setName(name);
        service.setDurationMinutes(30);
        service.setPrice(new BigDecimal("100.00"));
        service.setCapacity(1);
        return serviceRepository.saveAndFlush(service);
    }

    private void professional(Clinic clinic, ClinicUnit unit, ServiceOffering service, String name) {
        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setClinic(clinic);
        professional.setUnit(unit);
        professional.setName(name);
        professional.setAvailableWeekDaysValue("1,2,3,4,5,6,7");
        professional.setServices(new LinkedHashSet<>(List.of(service)));
        professionalRepository.saveAndFlush(professional);
    }

    private String bearer(User user) {
        return "Bearer " + token(user);
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }

    private String token(User user) {
        return tokenService.generateToken(user);
    }
}
