package com.agendamento.smart.service;

import com.agendamento.smart.PostgresIntegrationTestSupport;
import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.model.clinic.StatusClinic;
import com.agendamento.smart.model.professional.TypeProfessional;
import com.agendamento.smart.model.unit.StatusUnit;
import com.agendamento.smart.model.unit.TypeUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.ProfessionalRepository;
import com.agendamento.smart.repository.UnitRepository;
import com.agendamento.smart.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClinicOnboardingServiceIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private ClinicOnboardingService onboardingService;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createsActiveClinicMatrixAndInitialTeamAtomically() {
        ClinicRegistrationRequest request = request(
                "clinic-one-attendant@example.test",
                "clinic-one-admin@example.test");

        ClinicRegistrationResponse response = onboardingService.register(request);

        assertThat(response.clinicStatus()).isEqualTo(StatusClinic.ATIVO);
        assertThat(response.unitStatus()).isEqualTo(StatusUnit.ATIVO);
        assertThat(response.unitType()).isEqualTo(TypeUnit.MATRIZ);
        assertThat(response.attendant().role()).isEqualTo(UserRole.USER);
        assertThat(response.attendant().professionalType()).isEqualTo(TypeProfessional.ATENDENTE);
        assertThat(response.administrative().role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.administrative().professionalType()).isEqualTo(TypeProfessional.ADMINISTRATIVO);

        assertThat(clinicRepository.count()).isEqualTo(1);
        assertThat(unitRepository.countByClinicId(response.clinicId())).isEqualTo(1);
        assertThat(userRepository.countByClinicId(response.clinicId())).isEqualTo(2);
        assertThat(userRepository.countByClinicIdAndRole(response.clinicId(), UserRole.ADMIN)).isEqualTo(1);
        assertThat(professionalRepository.countByClinicId(response.clinicId())).isEqualTo(2);

        List<ProfessionalSummary> professionals = professionalRepository
                .findAllByClinicId(response.clinicId())
                .stream()
                .map(professional -> new ProfessionalSummary(
                        professional.getUnit().getId(),
                        professional.getUser().getId(),
                        professional.getAvailableWeekDaysValue()))
                .toList();
        assertThat(professionals)
                .allSatisfy(professional -> {
                    assertThat(professional.unitId()).isEqualTo(response.unitId());
                    assertThat(professional.userId()).isNotNull();
                    assertThat(professional.availableWeekDays()).isEmpty();
                });

        User attendant = userRepository.findById(response.attendant().userId()).orElseThrow();
        User administrative = userRepository.findById(response.administrative().userId()).orElseThrow();
        assertThat(passwordEncoder.matches("Attendant@123", attendant.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("Administrative@123", administrative.getPassword())).isTrue();
        assertThat(attendant.getPassword()).doesNotContain("Attendant@123");
        assertThat(administrative.getPassword()).doesNotContain("Administrative@123");
    }

    @Test
    void leavesNoPartialClinicWhenSecondLoginAlreadyExists() {
        ClinicRegistrationResponse existing = onboardingService.register(request(
                "existing-attendant@example.test",
                "existing-admin@example.test"));

        long clinicsBefore = clinicRepository.count();
        long unitsBefore = unitRepository.count();
        long usersBefore = userRepository.count();
        long professionalsBefore = professionalRepository.count();

        ClinicRegistrationRequest conflicting = request(
                "new-attendant@example.test",
                "EXISTING-ADMIN@example.test");

        assertThatThrownBy(() -> onboardingService.register(conflicting))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("LOGIN_ALREADY_EXISTS");

        assertThat(clinicRepository.count()).isEqualTo(clinicsBefore);
        assertThat(unitRepository.count()).isEqualTo(unitsBefore);
        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(professionalRepository.count()).isEqualTo(professionalsBefore);
        assertThat(clinicRepository.findAll())
                .extracting("id")
                .containsExactly(existing.clinicId());
    }

    private static ClinicRegistrationRequest request(String attendantLogin, String administrativeLogin) {
        return new ClinicRegistrationRequest(
                new ClinicRegistrationRequest.ClinicData("Clínica Teste"),
                new ClinicRegistrationRequest.MatrixUnitData("Matriz", "America/Cuiaba"),
                new ClinicRegistrationRequest.InitialProfessionalData(
                        "Atendente Inicial", attendantLogin, "Attendant@123"),
                new ClinicRegistrationRequest.InitialProfessionalData(
                        "Administrativo Inicial", administrativeLogin, "Administrative@123"));
    }

    private record ProfessionalSummary(
            java.util.UUID unitId,
            java.util.UUID userId,
            String availableWeekDays
    ) {
    }
}
