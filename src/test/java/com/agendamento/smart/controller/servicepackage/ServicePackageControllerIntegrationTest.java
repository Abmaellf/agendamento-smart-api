package com.agendamento.smart.controller.servicepackage;

import com.agendamento.smart.PostgresIntegrationTestSupport;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.infra.security.TokenService;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.ServiceOfferingRepository;
import com.agendamento.smart.repository.UnitRepository;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.ClinicOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServicePackageControllerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ClinicOnboardingService onboardingService;
    @Autowired private ClinicRepository clinicRepository;
    @Autowired private UnitRepository unitRepository;
    @Autowired private ServiceOfferingRepository serviceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TokenService tokenService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void createsFiltersUpdatesAndActivatesPackageWithoutDeleteEndpoint() throws Exception {
        Context context = context("catalog");
        String packageId = create(context, "Fisioterapia 10", List.of("MONDAY", "WEDNESDAY"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO SERVICE_PACKAGE_WEEK_DAY (service_package_id, week_day) VALUES (?, ?)",
                UUID.fromString(packageId), "MONDAY"))
                .isInstanceOf(DataIntegrityViolationException.class);

        mockMvc.perform(get("/api/service-packages")
                        .header("Authorization", bearer(context.admin()))
                        .param("unitId", context.unit().getId().toString())
                        .param("serviceId", context.service().getId().toString())
                        .param("totalSessions", "10")
                        .param("name", "fisioterapia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(packageId))
                .andExpect(jsonPath("$.content[0].totalPrice").value("400.00"))
                .andExpect(jsonPath("$.content[0].weeklyFrequency").value(2));

        mockMvc.perform(patch("/api/service-packages/{id}", packageId)
                        .header("Authorization", bearer(context.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request(context, "Fisioterapia revisada", List.of("MONDAY", "WEDNESDAY", "FRIDAY")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fisioterapia revisada"))
                .andExpect(jsonPath("$.weeklyFrequency").value(3));

        mockMvc.perform(patch("/api/service-packages/{id}/status", packageId)
                        .header("Authorization", bearer(context.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/service-packages")
                        .header("Authorization", bearer(context.admin()))
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void rejectsDuplicateDaysBasicUserAndPackageFromAnotherClinic() throws Exception {
        Context owner = context("owner");
        Context anotherClinic = context("another-clinic");
        String packageId = create(owner, "Pacote proprietário", List.of("MONDAY"));

        mockMvc.perform(post("/api/service-packages")
                        .header("Authorization", bearer(owner.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request(owner, "Dias repetidos", List.of("MONDAY", "MONDAY")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PACKAGE_VALIDATION_ERROR"));

        mockMvc.perform(get("/api/service-packages")
                        .header("Authorization", bearer(owner.attendant())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(packageId));

        mockMvc.perform(get("/api/service-packages")
                        .header("Authorization", bearer(owner.attendant()))
                        .param("unitId", anotherClinic.unit().getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TENANT_RESOURCE_NOT_FOUND"));

        mockMvc.perform(post("/api/service-packages")
                        .header("Authorization", bearer(owner.attendant()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request(owner, "Pacote BASIC", List.of("MONDAY")))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/service-packages/{id}", packageId)
                        .header("Authorization", bearer(anotherClinic.admin())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PACKAGE_NOT_FOUND"));
    }

    @Test
    void paginatesFiltersAndValidatesPackageListingParameters() throws Exception {
        Context context = context("pagination");
        create(context, "A Pacote", List.of("MONDAY"));
        create(context, "B Pacote", List.of("WEDNESDAY"));

        mockMvc.perform(get("/api/service-packages")
                        .header("Authorization", bearer(context.admin()))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("A Pacote"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/service-packages")
                        .header("Authorization", bearer(context.admin()))
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("B Pacote"))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(get("/api/service-packages")
                        .header("Authorization", bearer(context.admin()))
                        .param("name", "b pacote")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("B Pacote"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/service-packages")
                        .header("Authorization", bearer(context.admin()))
                        .param("name", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(get("/api/service-packages")
                        .header("Authorization", bearer(context.admin()))
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PACKAGE_PAGINATION_ERROR"));

        mockMvc.perform(get("/api/service-packages")
                        .header("Authorization", bearer(context.admin()))
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PACKAGE_PAGINATION_ERROR"));
    }

    private String create(Context context, String name, List<String> weekDays) throws Exception {
        String body = mockMvc.perform(post("/api/service-packages")
                        .header("Authorization", bearer(context.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request(context, name, weekDays))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weekDays", hasSize(weekDays.size())))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private Map<String, Object> request(Context context, String name, List<String> weekDays) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", name);
        request.put("description", "Descrição do pacote");
        request.put("unitId", context.unit().getId());
        request.put("serviceId", context.service().getId());
        request.put("totalSessions", 10);
        request.put("totalPrice", "400.00");
        request.put("weekDays", weekDays);
        request.put("active", true);
        return request;
    }

    private Context context(String suffix) {
        ClinicRegistrationResponse registration = onboardingService.register(new ClinicRegistrationRequest(
                new ClinicRegistrationRequest.ClinicData("Clínica pacote " + suffix),
                new ClinicRegistrationRequest.MatrixUnitData("Matriz " + suffix, "America/Cuiaba"),
                new ClinicRegistrationRequest.InitialProfessionalData("Atendente " + suffix, "attendant-package-" + suffix + "@example.test", "Attendant@123"),
                new ClinicRegistrationRequest.InitialProfessionalData("Administrativo " + suffix, "admin-package-" + suffix + "@example.test", "Administrative@123")));
        Clinic clinic = clinicRepository.findById(registration.clinicId()).orElseThrow();
        ClinicUnit unit = unitRepository.findById(registration.unitId()).orElseThrow();
        ServiceOffering service = new ServiceOffering();
        service.setId(UUID.randomUUID());
        service.setClinic(clinic);
        service.setName("Fisioterapia " + suffix);
        service.setDurationMinutes(30);
        service.setPrice(new BigDecimal("125.00"));
        service.setCapacity(1);
        service = serviceRepository.saveAndFlush(service);
        return new Context(unit, service,
                userRepository.findById(registration.administrative().userId()).orElseThrow(),
                userRepository.findById(registration.attendant().userId()).orElseThrow());
    }

    private String bearer(User user) {
        return "Bearer " + tokenService.generateToken(user);
    }

    private record Context(ClinicUnit unit, ServiceOffering service, User admin, User attendant) { }
}
