package com.agendamento.smart.controller.auth;

import com.agendamento.smart.PostgresIntegrationTestSupport;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("prod")
class AuthenticationCookieIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void productionLoginEmitsSecureHttpOnlyCookieUsableForAuthenticatedRequest() throws Exception {
        mockMvc.perform(post("/auth/register/clinic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(registrationRequest())))
                .andExpect(status().isCreated());

        var login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"admin-proxy@example.test","password":"Administrative@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Secure")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andReturn();

        var jwtCookie = login.getResponse().getCookie("jwt");

        mockMvc.perform(get("/auth/me").cookie(jwtCookie))
                .andExpect(status().isOk());
    }

    private static ClinicRegistrationRequest registrationRequest() {
        return new ClinicRegistrationRequest(
                new ClinicRegistrationRequest.ClinicData("Clínica Proxy"),
                new ClinicRegistrationRequest.MatrixUnitData("Matriz Proxy", "America/Cuiaba"),
                new ClinicRegistrationRequest.InitialProfessionalData(
                        "Atendente Proxy", "attendant-proxy@example.test", "Attendant@123"),
                new ClinicRegistrationRequest.InitialProfessionalData(
                        "Administrativo Proxy", "admin-proxy@example.test", "Administrative@123"));
    }
}
