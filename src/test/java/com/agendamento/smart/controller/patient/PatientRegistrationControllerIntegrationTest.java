package com.agendamento.smart.controller.patient;

import com.agendamento.smart.PostgresIntegrationTestSupport;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.infra.security.TokenService;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.unit.StatusUnit;
import com.agendamento.smart.model.unit.TypeUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.UnitRepository;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.ClinicOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PatientRegistrationControllerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicOnboardingService onboardingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    void createsUppercasePatientInOneUnitUsingAuthenticatedClinic() throws Exception {
        Context context = clinic("one-unit");
        PatientRequestDTO request = new PatientRequestDTO(
                "Paciente Um", context.registration().clinicId(), List.of(context.registration().unitId()));

        String response = mockMvc.perform(post("/patient/save")
                        .header("Authorization", bearer(context.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PACIENTE UM"))
                .andExpect(jsonPath("$.clinicId").value(context.registration().clinicId().toString()))
                .andExpect(jsonPath("$.unitIds[0]").value(context.registration().unitId().toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID patientId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        Patient persisted = patientRepository.findByIdWithUnits(patientId).orElseThrow();
        assertThat(persisted.getUnits()).extracting(ClinicUnit::getId)
                .containsExactly(context.registration().unitId());
    }

    @Test
    void createsPatientInTwoUnitsOfSameClinic() throws Exception {
        Context context = clinic("two-units");
        ClinicUnit filial = filial(context);
        PatientRequestDTO request = new PatientRequestDTO(
                "Paciente Duas Unidades",
                null,
                List.of(context.registration().unitId(), filial.getId()));

        String response = mockMvc.perform(post("/patient/save")
                        .header("Authorization", bearer(context.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitIds.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID patientId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        Patient persisted = patientRepository.findByIdWithUnits(patientId).orElseThrow();
        assertThat(persisted.getUnits()).extracting(ClinicUnit::getId)
                .containsExactlyInAnyOrder(context.registration().unitId(), filial.getId());
    }

    @Test
    void rejectsEmptyUnitListAndForeignUnit() throws Exception {
        Context first = clinic("patient-first");
        Context second = clinic("patient-second");

        PatientRequestDTO empty = new PatientRequestDTO("Sem Unidade", null, List.of());
        mockMvc.perform(post("/patient/save")
                        .header("Authorization", bearer(first.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(empty)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        PatientRequestDTO foreign = new PatientRequestDTO(
                "Unidade Alheia", null, List.of(second.registration().unitId()));
        mockMvc.perform(post("/patient/save")
                        .header("Authorization", bearer(first.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(foreign)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TENANT_RESOURCE_NOT_FOUND"));

        assertThat(patientRepository.count()).isZero();
    }

    @Test
    void rejectsLegacyClinicIdFromAnotherClinic() throws Exception {
        Context first = clinic("legacy-first");
        Context second = clinic("legacy-second");
        PatientRequestDTO request = new PatientRequestDTO(
                "Clinic Divergente",
                second.registration().clinicId(),
                List.of(first.registration().unitId()));

        mockMvc.perform(post("/patient/save")
                        .header("Authorization", bearer(first.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLINIC_ACCESS_DENIED"));

        assertThat(patientRepository.count()).isZero();
    }

    @Test
    void keepsGlobalDuplicateNameRule() throws Exception {
        Context first = clinic("duplicate-patient-first");
        Context second = clinic("duplicate-patient-second");
        PatientRequestDTO firstRequest = new PatientRequestDTO(
                "Nome Global", null, List.of(first.registration().unitId()));
        mockMvc.perform(post("/patient/save")
                        .header("Authorization", bearer(first.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(firstRequest)))
                .andExpect(status().isOk());

        PatientRequestDTO duplicate = new PatientRequestDTO(
                "nome global", null, List.of(second.registration().unitId()));
        mockMvc.perform(post("/patient/save")
                        .header("Authorization", bearer(second.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PATIENT_ALREADY_EXISTS"));

        assertThat(patientRepository.count()).isEqualTo(1);
    }

    private Context clinic(String suffix) {
        ClinicRegistrationResponse registration = onboardingService.register(
                new ClinicRegistrationRequest(
                        new ClinicRegistrationRequest.ClinicData("Clínica " + suffix),
                        new ClinicRegistrationRequest.MatrixUnitData("Matriz " + suffix, "America/Cuiaba"),
                        new ClinicRegistrationRequest.InitialProfessionalData(
                                "Atendente " + suffix,
                                "attendant-" + suffix + "@example.test",
                                "Attendant@123"),
                        new ClinicRegistrationRequest.InitialProfessionalData(
                                "Administrativo " + suffix,
                                "admin-" + suffix + "@example.test",
                                "Administrative@123")));
        User admin = userRepository.findById(registration.administrative().userId()).orElseThrow();
        return new Context(registration, admin);
    }

    private ClinicUnit filial(Context context) {
        ClinicUnit unit = new ClinicUnit();
        unit.setId(UUID.randomUUID());
        unit.setClinic(context.admin().getClinic());
        unit.setTenantId(context.registration().clinicId());
        unit.setName("Filial de teste");
        unit.setTimeZone("America/Cuiaba");
        unit.setStatus(StatusUnit.ATIVO);
        unit.setType(TypeUnit.FILIAL);
        return unitRepository.saveAndFlush(unit);
    }

    private String bearer(User user) {
        return "Bearer " + tokenService.generateToken(user);
    }

    private record Context(ClinicRegistrationResponse registration, User admin) {
    }
}
