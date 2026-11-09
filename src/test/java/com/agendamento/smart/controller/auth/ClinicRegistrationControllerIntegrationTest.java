package com.agendamento.smart.controller.auth;

import com.agendamento.smart.PostgresIntegrationTestSupport;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClinicRegistrationControllerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registersClinicPubliclyAndReturnsCreatedWithoutCredentials() throws Exception {
        ClinicRegistrationRequest request = validRequest();

        MvcResult result = mockMvc.perform(post("/auth/register/clinic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clinicId").isNotEmpty())
                .andExpect(jsonPath("$.clinicStatus").value("ATIVO"))
                .andExpect(jsonPath("$.unitStatus").value("ATIVO"))
                .andExpect(jsonPath("$.unitType").value("MATRIZ"))
                .andExpect(jsonPath("$.attendant.role").value("USER"))
                .andExpect(jsonPath("$.attendant.professionalType").value("ATENDENTE"))
                .andExpect(jsonPath("$.administrative.role").value("ADMIN"))
                .andExpect(jsonPath("$.administrative.professionalType").value("ADMINISTRATIVO"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .doesNotContainIgnoringCase("password")
                .doesNotContainIgnoringCase("login")
                .doesNotContain("Attendant@123")
                .doesNotContain("Administrative@123")
                .doesNotContain("$2a$")
                .doesNotContain("$2b$");
        assertThat(clinicRepository.count()).isEqualTo(1);
        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidPayloadWithoutPersistingAnything() throws Exception {
        String invalidPayload = """
                {
                  "clinic": {"name": ""},
                  "matrix": {"name": "", "timeZone": ""},
                  "attendant": {"name": "", "login": "", "password": "short"}
                }
                """;

        mockMvc.perform(post("/auth/register/clinic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(clinicRepository.count()).isZero();
        assertThat(userRepository.count()).isZero();
    }

    private static ClinicRegistrationRequest validRequest() {
        return new ClinicRegistrationRequest(
                new ClinicRegistrationRequest.ClinicData("Clínica HTTP"),
                new ClinicRegistrationRequest.MatrixUnitData("Matriz HTTP", "America/Cuiaba"),
                new ClinicRegistrationRequest.InitialProfessionalData(
                        "Atendente HTTP", "attendant-http@example.test", "Attendant@123"),
                new ClinicRegistrationRequest.InitialProfessionalData(
                        "Administrativo HTTP", "admin-http@example.test", "Administrative@123"));
    }
}
