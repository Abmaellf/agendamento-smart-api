package com.agendamento.smart.controller.patient;

import com.agendamento.smart.PostgresIntegrationTestSupport;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.infra.security.TokenService;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.unit.StatusUnit;
import com.agendamento.smart.model.unit.TypeUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.UnitRepository;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.ClinicOnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PatientListingControllerIntegrationTest
        extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

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
    void adminListsOnlyOwnClinicWithRealPagination() throws Exception {
        Context own = clinic("list-own");
        Context foreign = clinic("list-foreign");
        ClinicUnit ownMatrix = matrix(own);
        ClinicUnit foreignMatrix = matrix(foreign);

        patient(own, ownMatrix, "ANA PACIENTE");
        patient(own, ownMatrix, "BIA PACIENTE");
        patient(foreign, foreignMatrix, "PACIENTE ESTRANGEIRO");

        mockMvc.perform(get("/patient/list")
                        .header("Authorization", bearer(own.admin()))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content[0].code").isNumber())
                .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.content[0].status").value(nullValue()))
                .andExpect(jsonPath("$.content[0].principalService")
                        .value(nullValue()));

        mockMvc.perform(get("/patient/list")
                        .header("Authorization", bearer(own.admin()))
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void filtersNameInsideAuthenticatedClinic() throws Exception {
        Context context = clinic("name-filter");
        ClinicUnit matrix = matrix(context);
        patient(context, matrix, "ANA OLIVEIRA");
        patient(context, matrix, "BRUNO SANTOS");

        mockMvc.perform(get("/patient/list")
                        .header("Authorization", bearer(context.admin()))
                        .param("name", " ana ")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name")
                        .value("ANA OLIVEIRA"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void basicListsOnlyPatientsFromAssociatedUnit() throws Exception {
        Context context = clinic("basic-list");
        ClinicUnit matrix = matrix(context);
        ClinicUnit branch = branch(context, "Filial Basic");
        User basic = basic(context, branch, "list");

        patient(context, matrix, "PACIENTE MATRIZ");
        Patient allowed = patient(context, branch, "PACIENTE FILIAL");

        mockMvc.perform(get("/patient/list")
                        .header("Authorization", bearer(basic))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id")
                        .value(allowed.getId().toString()))
                .andExpect(jsonPath("$.content[0].name")
                        .value("PACIENTE FILIAL"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void basicNameFilterDoesNotEscapeUnitScope() throws Exception {
        Context context = clinic("basic-filter");
        ClinicUnit matrix = matrix(context);
        ClinicUnit branch = branch(context, "Filial Filtro");
        User basic = basic(context, branch, "filter");

        patient(context, matrix, "ANA FORA DA UNIDADE");
        patient(context, branch, "BRUNO NA UNIDADE");

        mockMvc.perform(get("/patient/list")
                        .header("Authorization", bearer(basic))
                        .param("name", "ANA")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsAnonymousListing() throws Exception {
        mockMvc.perform(get("/patient/list"))
                .andExpect(status().isForbidden());
    }

    private Context clinic(String suffix) {
        ClinicRegistrationResponse registration = onboardingService.register(
                new ClinicRegistrationRequest(
                        new ClinicRegistrationRequest.ClinicData(
                                "Clínica " + suffix),
                        new ClinicRegistrationRequest.MatrixUnitData(
                                "Matriz " + suffix,
                                "America/Cuiaba"),
                        new ClinicRegistrationRequest.InitialProfessionalData(
                                "Atendente " + suffix,
                                "attendant-" + suffix + "@example.test",
                                "Attendant@123"),
                        new ClinicRegistrationRequest.InitialProfessionalData(
                                "Administrativo " + suffix,
                                "admin-" + suffix + "@example.test",
                                "Administrative@123")));
        User admin = userRepository
                .findById(registration.administrative().userId())
                .orElseThrow();
        return new Context(registration, admin);
    }

    private ClinicUnit matrix(Context context) {
        return unitRepository
                .findById(context.registration().unitId())
                .orElseThrow();
    }

    private ClinicUnit branch(Context context, String name) {
        ClinicUnit unit = new ClinicUnit();
        unit.setId(UUID.randomUUID());
        unit.setClinic(context.admin().getClinic());
        unit.setTenantId(context.registration().clinicId());
        unit.setName(name);
        unit.setTimeZone("America/Cuiaba");
        unit.setStatus(StatusUnit.ATIVO);
        unit.setType(TypeUnit.FILIAL);
        return unitRepository.saveAndFlush(unit);
    }

    private User basic(
            Context context,
            ClinicUnit unit,
            String suffix) {
        User user = User.builder()
                .login("basic-list-" + suffix + "@example.test")
                .password("not-used-by-token-test")
                .role(UserRole.USER)
                .clinic(context.admin().getClinic())
                .clinicUnit(unit)
                .build();
        return userRepository.saveAndFlush(user);
    }

    private Patient patient(
            Context context,
            ClinicUnit unit,
            String name) {
        Patient patient = new Patient();
        patient.setName(name);
        patient.setClinic(context.admin().getClinic());
        patient.setUnits(new LinkedHashSet<>(List.of(unit)));
        return patientRepository.saveAndFlush(patient);
    }

    private String bearer(User user) {
        return "Bearer " + tokenService.generateToken(user);
    }

    private record Context(
            ClinicRegistrationResponse registration,
            User admin) {
    }
}
