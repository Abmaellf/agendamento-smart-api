package com.agendamento.smart.infra.security;

import com.agendamento.smart.PostgresIntegrationTestSupport;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.dtos.user.RegisterDTO;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.ClinicOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("prod")
class CorsConfigurationIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String COMPROMIZ_ORIGIN = "https://compromiz.com.br";
    private static final List<String> EXISTING_ORIGINS = List.of(
            "https://agendamentos-smart.vercel.app",
            "http://localhost:3000",
            "http://localhost:5173");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicOnboardingService onboardingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    void allowsCompromizAcrossGlobalAndControllerSpecificPolicies() throws Exception {
        Context context = clinic();

        assertAllowedPreflight("/auth/login", "POST", "content-type");
        assertAllowedPreflight(
                "/user/register/" + context.registration().clinicId(),
                "POST",
                "authorization,content-type");
        assertAllowedPreflight("/api/units", "GET", "authorization");

        mockMvc.perform(post("/auth/login")
                        .header("Origin", COMPROMIZ_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"admin-cors@example.test","password":"Administrative@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", COMPROMIZ_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        RegisterDTO request = new RegisterDTO(
                "new-cors@example.test",
                "NewCors@123",
                UserRole.USER,
                context.registration().unitId(),
                false,
                null);
        mockMvc.perform(post("/user/register/{clinicId}", context.registration().clinicId())
                        .header("Origin", COMPROMIZ_ORIGIN)
                        .header("Authorization", bearer(context.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", COMPROMIZ_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(get("/api/units")
                        .header("Origin", COMPROMIZ_ORIGIN)
                        .header("Authorization", bearer(context.admin())))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", COMPROMIZ_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void preservesExistingOriginsAndRejectsUnknownOrigins() throws Exception {
        for (String origin : EXISTING_ORIGINS) {
            mockMvc.perform(options("/api/units")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "GET")
                            .header("Access-Control-Request-Headers", "authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }

        mockMvc.perform(options("/api/units")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    private void assertAllowedPreflight(String path, String method, String headers) throws Exception {
        mockMvc.perform(options(path)
                        .header("Origin", COMPROMIZ_ORIGIN)
                        .header("Access-Control-Request-Method", method)
                        .header("Access-Control-Request-Headers", headers))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", COMPROMIZ_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    private Context clinic() {
        ClinicRegistrationResponse registration = onboardingService.register(
                new ClinicRegistrationRequest(
                        new ClinicRegistrationRequest.ClinicData("Clínica CORS"),
                        new ClinicRegistrationRequest.MatrixUnitData("Matriz CORS", "America/Cuiaba"),
                        new ClinicRegistrationRequest.InitialProfessionalData(
                                "Atendente CORS", "attendant-cors@example.test", "Attendant@123"),
                        new ClinicRegistrationRequest.InitialProfessionalData(
                                "Administrativo CORS", "admin-cors@example.test", "Administrative@123")));
        User admin = userRepository.findById(registration.administrative().userId()).orElseThrow();
        return new Context(registration, admin);
    }

    private String bearer(User user) {
        return "Bearer " + tokenService.generateToken(user);
    }

    private record Context(ClinicRegistrationResponse registration, User admin) {
    }
}
