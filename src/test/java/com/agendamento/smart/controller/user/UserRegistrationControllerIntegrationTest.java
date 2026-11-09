package com.agendamento.smart.controller.user;

import com.agendamento.smart.PostgresIntegrationTestSupport;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.dtos.user.RegisterDTO;
import com.agendamento.smart.infra.security.TokenService;
import com.agendamento.smart.model.professional.Professional;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.ProfessionalRepository;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.ClinicOnboardingService;
import com.agendamento.smart.service.UserRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserRegistrationControllerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicOnboardingService onboardingService;

    @Autowired
    private UserRegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Test
    void createsOnlyUserWhenIsProfessionalIsFalseAndEncodesPassword() throws Exception {
        Context context = clinic("one");
        long professionalsBefore = professionalRepository.count();
        RegisterDTO request = new RegisterDTO(
                "new-user@example.test", "NewUser@123", UserRole.USER, false, null);

        mockMvc.perform(post("/user/register/{clinicId}", context.registration().clinicId())
                        .header("Authorization", bearer(context.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk());

        User created = (User) userRepository.findByLogin("NEW-USER@example.test");
        assertThat(created).isNotNull();
        assertThat(created.getRole()).isEqualTo(UserRole.USER);
        assertThat(created.getClinic().getId()).isEqualTo(context.registration().clinicId());
        assertThat(passwordEncoder.matches("NewUser@123", created.getPassword())).isTrue();
        assertThat(professionalRepository.count()).isEqualTo(professionalsBefore);
    }

    @Test
    void createsUserAndProfessionalInMatrixWhenIsProfessionalIsTrue() throws Exception {
        Context context = clinic("professional");
        RegisterDTO request = new RegisterDTO(
                "clinical-professional@example.test",
                "Professional@123",
                UserRole.USER,
                true,
                new RegisterDTO.ProfessionalData("Profissional Clínico", Set.of(5, 1, 3)));

        mockMvc.perform(post("/user/register/{clinicId}", context.registration().clinicId())
                        .header("Authorization", bearer(context.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk());

        User created = (User) userRepository.findByLogin("clinical-professional@example.test");
        Professional professional = professionalRepository.findByUserId(created.getId()).orElseThrow();
        assertThat(professional.getClinic().getId()).isEqualTo(context.registration().clinicId());
        assertThat(professional.getUnit().getId()).isEqualTo(context.registration().unitId());
        assertThat(professional.getAvailableWeekDaysValue()).isEqualTo("1,3,5");
        assertThat(professional.getType()).isNull();
        assertThat(passwordEncoder.matches("Professional@123", created.getPassword())).isTrue();
    }

    @Test
    void rejectsAnotherClinicAndAdminRole() throws Exception {
        Context first = clinic("first");
        Context second = clinic("second");
        long usersBefore = userRepository.count();

        RegisterDTO otherClinic = new RegisterDTO(
                "foreign@example.test", "Foreign@123", UserRole.USER, false, null);
        mockMvc.perform(post("/user/register/{clinicId}", second.registration().clinicId())
                        .header("Authorization", bearer(first.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(otherClinic)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLINIC_ACCESS_DENIED"));

        RegisterDTO secondAdmin = new RegisterDTO(
                "second-admin@example.test", "SecondAdmin@123", UserRole.ADMIN, false, null);
        mockMvc.perform(post("/user/register/{clinicId}", first.registration().clinicId())
                        .header("Authorization", bearer(first.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(secondAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ROLE_NOT_ALLOWED"));

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(userRepository.countByClinicIdAndRole(
                first.registration().clinicId(), UserRole.ADMIN)).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateLoginAndMissingProfessionalChoice() throws Exception {
        Context context = clinic("duplicate");
        long usersBefore = userRepository.count();

        RegisterDTO duplicate = new RegisterDTO(
                context.admin().getLogin().toUpperCase(),
                "Duplicate@123",
                UserRole.USER,
                false,
                null);
        mockMvc.perform(post("/user/register/{clinicId}", context.registration().clinicId())
                        .header("Authorization", bearer(context.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOGIN_ALREADY_EXISTS"));

        String missingChoice = """
                {
                  "login": "missing-choice@example.test",
                  "password": "MissingChoice@123",
                  "role": "USER"
                }
                """;
        mockMvc.perform(post("/user/register/{clinicId}", context.registration().clinicId())
                        .header("Authorization", bearer(context.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingChoice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(userRepository.count()).isEqualTo(usersBefore);
    }

    @Test
    void rollsBackUserWhenProfessionalPersistenceFails() {
        Context context = clinic("rollback");
        long usersBefore = userRepository.count();
        long professionalsBefore = professionalRepository.count();
        RegisterDTO invalidProfessional = new RegisterDTO(
                "rollback-user@example.test",
                "Rollback@123",
                UserRole.USER,
                true,
                new RegisterDTO.ProfessionalData("X".repeat(256), Set.of(1)));
        var authentication = new UsernamePasswordAuthenticationToken(
                context.admin(), null, context.admin().getAuthorities());

        assertThatThrownBy(() -> registrationService.register(
                context.registration().clinicId(), invalidProfessional, authentication))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(professionalRepository.count()).isEqualTo(professionalsBefore);
        assertThat(userRepository.existsByLoginIgnoreCase("rollback-user@example.test")).isFalse();
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

    private String bearer(User user) {
        return "Bearer " + tokenService.generateToken(user);
    }

    private record Context(ClinicRegistrationResponse registration, User admin) {
    }
}
