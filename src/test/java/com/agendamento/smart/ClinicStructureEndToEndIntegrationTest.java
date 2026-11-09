package com.agendamento.smart;

import com.agendamento.smart.dtos.AuthenticationDTO;
import com.agendamento.smart.dtos.appointment.AppointmentRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.dtos.user.RegisterDTO;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.clinic.StatusClinic;
import com.agendamento.smart.model.professional.Professional;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.unit.StatusUnit;
import com.agendamento.smart.model.unit.TypeUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.AppointmentRepository;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.ProfessionalRepository;
import com.agendamento.smart.repository.ServiceOfferingRepository;
import com.agendamento.smart.repository.UnitRepository;
import com.agendamento.smart.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClinicStructureEndToEndIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String ADMIN_PASSWORD = "Administrative@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void runsTwoClinicWorkflowWithInitialTeamLinksAndIsolatedAppointmentSearches() throws Exception {
        ClinicRegistrationResponse first = registerClinic("integrated-first");
        ClinicRegistrationResponse second = registerClinic("integrated-second");

        assertInitialStructure(first);
        assertInitialStructure(second);
        String firstToken = login("admin-integrated-first@example.test");
        String secondToken = login("admin-integrated-second@example.test");

        RegisterDTO professionalRequest = new RegisterDTO(
                "clinical-integrated-first@example.test",
                "Professional@123",
                UserRole.USER,
                true,
                new RegisterDTO.ProfessionalData(
                        "Profissional Integrado",
                        Set.of(1, 2, 3, 4, 5, 6, 7)));
        mockMvc.perform(post("/user/register/{clinicId}", first.clinicId())
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(professionalRequest)))
                .andExpect(status().isOk());

        PatientResponseDTO firstPatient = registerPatient(
                firstToken, "Paciente Integrado Um", first.unitId());
        PatientResponseDTO secondPatient = registerPatient(
                secondToken, "Paciente Integrado Dois", second.unitId());
        ServiceOffering firstService = service(first.clinicId(), "Serviço Integrado Um");
        ServiceOffering secondService = service(second.clinicId(), "Serviço Integrado Dois");

        User clinicalUser = (User) userRepository.findByLogin(
                "clinical-integrated-first@example.test");
        Professional clinicalProfessional = professionalRepository
                .findByUserId(clinicalUser.getId())
                .orElseThrow();
        clinicalProfessional.setServices(new LinkedHashSet<>(Set.of(firstService)));
        professionalRepository.saveAndFlush(clinicalProfessional);

        Instant firstStartsAt = Instant.now()
                .plus(10, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.SECONDS);
        Instant secondStartsAt = firstStartsAt.plus(1, ChronoUnit.HOURS);
        UUID firstAppointmentId = createAppointment(
                firstToken,
                firstPatient.id(),
                first.unitId(),
                firstService,
                clinicalProfessional.getId(),
                firstStartsAt);
        UUID secondAppointmentId = createAppointment(
                secondToken,
                secondPatient.id(),
                second.unitId(),
                secondService,
                null,
                secondStartsAt);

        Instant from = firstStartsAt.minus(1, ChronoUnit.HOURS);
        Instant to = secondStartsAt.plus(1, ChronoUnit.HOURS);
        mockMvc.perform(get("/api/appointments")
                        .header("Authorization", bearer(firstToken))
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(firstAppointmentId.toString()));

        mockMvc.perform(get("/api/appointments")
                        .header("Authorization", bearer(firstToken))
                        .param("unitId", first.unitId().toString())
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(firstAppointmentId.toString()));

        mockMvc.perform(get("/api/appointments")
                        .header("Authorization", bearer(firstToken))
                        .param("unitId", second.unitId().toString())
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TENANT_RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/api/appointments/{id}", secondAppointmentId)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TENANT_RESOURCE_NOT_FOUND"));

        assertThat(userRepository.countByClinicIdAndRole(first.clinicId(), UserRole.ADMIN)).isEqualTo(1);
        assertThat(userRepository.countByClinicIdAndRole(second.clinicId(), UserRole.ADMIN)).isEqualTo(1);
        assertThat(unitRepository.findAllByClinicId(first.clinicId()))
                .singleElement()
                .satisfies(unit -> assertThat(unit.getType()).isEqualTo(TypeUnit.MATRIZ));
        assertThat(unitRepository.findAllByClinicId(second.clinicId()))
                .singleElement()
                .satisfies(unit -> assertThat(unit.getType()).isEqualTo(TypeUnit.MATRIZ));
        assertThat(clinicalProfessional.getClinic().getId()).isEqualTo(first.clinicId());
        assertThat(clinicalProfessional.getUnit().getId()).isEqualTo(first.unitId());
        assertThat(patientRepository.findByIdWithUnits(firstPatient.id()).orElseThrow().getUnits())
                .extracting(unit -> unit.getId())
                .containsExactly(first.unitId());
        assertThat(appointmentRepository.count()).isEqualTo(2);
    }

    private ClinicRegistrationResponse registerClinic(String suffix) throws Exception {
        ClinicRegistrationRequest request = new ClinicRegistrationRequest(
                new ClinicRegistrationRequest.ClinicData("Clínica " + suffix),
                new ClinicRegistrationRequest.MatrixUnitData("Matriz " + suffix, "America/Cuiaba"),
                new ClinicRegistrationRequest.InitialProfessionalData(
                        "Atendente " + suffix,
                        "attendant-" + suffix + "@example.test",
                        "Attendant@123"),
                new ClinicRegistrationRequest.InitialProfessionalData(
                        "Administrativo " + suffix,
                        "admin-" + suffix + "@example.test",
                        ADMIN_PASSWORD));

        String response = mockMvc.perform(post("/auth/register/clinic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, ClinicRegistrationResponse.class);
    }

    private void assertInitialStructure(ClinicRegistrationResponse registration) {
        assertThat(registration.clinicStatus()).isEqualTo(StatusClinic.ATIVO);
        assertThat(registration.unitStatus()).isEqualTo(StatusUnit.ATIVO);
        assertThat(registration.unitType()).isEqualTo(TypeUnit.MATRIZ);
        assertThat(userRepository.countByClinicId(registration.clinicId())).isEqualTo(2);
        assertThat(userRepository.countByClinicIdAndRole(
                registration.clinicId(), UserRole.ADMIN)).isEqualTo(1);
        assertThat(professionalRepository.countByClinicId(registration.clinicId())).isEqualTo(2);
        assertThat(unitRepository.findAllByClinicId(registration.clinicId())).hasSize(1);
    }

    private String login(String login) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new AuthenticationDTO(login, ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private PatientResponseDTO registerPatient(String token, String name, UUID unitId) throws Exception {
        PatientRequestDTO request = new PatientRequestDTO(name, null, List.of(unitId));
        String response = mockMvc.perform(post("/patient/save")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, PatientResponseDTO.class);
    }

    private ServiceOffering service(UUID clinicId, String name) {
        Clinic clinic = clinicRepository.findById(clinicId).orElseThrow();
        ServiceOffering service = new ServiceOffering();
        service.setId(UUID.randomUUID());
        service.setClinic(clinic);
        service.setName(name);
        service.setDurationMinutes(30);
        service.setPrice(new BigDecimal("125.00"));
        service.setCapacity(5);
        return serviceOfferingRepository.saveAndFlush(service);
    }

    private UUID createAppointment(
            String token,
            UUID patientId,
            UUID unitId,
            ServiceOffering service,
            UUID professionalId,
            Instant startsAt) throws Exception {
        AppointmentRequest request = new AppointmentRequest(
                patientId,
                unitId,
                service.getId(),
                professionalId,
                startsAt,
                service.getDurationMinutes(),
                service.getPrice());
        String response = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return UUID.fromString(body.get("id").asText());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
