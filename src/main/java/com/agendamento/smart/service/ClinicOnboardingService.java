package com.agendamento.smart.service;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.clinic.StatusClinic;
import com.agendamento.smart.model.professional.Professional;
import com.agendamento.smart.model.professional.TypeProfessional;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.unit.StatusUnit;
import com.agendamento.smart.model.unit.TypeUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.ProfessionalRepository;
import com.agendamento.smart.repository.UnitRepository;
import com.agendamento.smart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicOnboardingService {

    private final ClinicRepository clinicRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ClinicRegistrationResponse register(ClinicRegistrationRequest request) {
        String attendantLogin = normalizeLogin(request.attendant().login());
        String administrativeLogin = normalizeLogin(request.administrative().login());

        if (attendantLogin.equals(administrativeLogin)) {
            throw loginConflict();
        }
        assertLoginAvailable(attendantLogin);
        assertLoginAvailable(administrativeLogin);
        validateTimeZone(request.matrix().timeZone());

        Clinic clinic = Clinic.builder()
                .name(request.clinic().name().trim())
                .status(StatusClinic.ATIVO)
                .build();
        clinic = clinicRepository.saveAndFlush(clinic);

        ClinicUnit matrix = new ClinicUnit();
        matrix.setId(UUID.randomUUID());
        matrix.setClinic(clinic);
        matrix.setTenantId(clinic.getId());
        matrix.setName(request.matrix().name().trim());
        matrix.setTimeZone(request.matrix().timeZone().trim());
        matrix.setStatus(StatusUnit.ATIVO);
        matrix.setType(TypeUnit.MATRIZ);
        matrix = unitRepository.save(matrix);

        InitialTeamMember attendant = createTeamMember(
                clinic,
                matrix,
                request.attendant(),
                attendantLogin,
                UserRole.USER,
                TypeProfessional.ATENDENTE);
        InitialTeamMember administrative = createTeamMember(
                clinic,
                matrix,
                request.administrative(),
                administrativeLogin,
                UserRole.ADMIN,
                TypeProfessional.ADMINISTRATIVO);

        return new ClinicRegistrationResponse(
                clinic.getId(),
                clinic.getStatus(),
                matrix.getId(),
                matrix.getStatus(),
                matrix.getType(),
                attendant.toResponse(),
                administrative.toResponse());
    }

    private InitialTeamMember createTeamMember(
            Clinic clinic,
            ClinicUnit unit,
            ClinicRegistrationRequest.InitialProfessionalData data,
            String login,
            UserRole role,
            TypeProfessional type
    ) {
        User user = User.builder()
                .login(login)
                .password(passwordEncoder.encode(data.password()))
                .role(role)
                .clinic(clinic)
                .build();
        user = userRepository.saveAndFlush(user);

        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setClinic(clinic);
        professional.setName(data.name().trim());
        professional.setAvailableWeekDaysValue("");
        professional.setUnit(unit);
        professional.setUser(user);
        professional.setType(type);
        professional.setServices(new LinkedHashSet<>());
        professional = professionalRepository.save(professional);

        return new InitialTeamMember(user, professional);
    }

    private void assertLoginAvailable(String login) {
        if (userRepository.existsByLoginIgnoreCase(login)) {
            throw loginConflict();
        }
    }

    private static String normalizeLogin(String login) {
        return login.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateTimeZone(String timeZone) {
        try {
            ZoneId.of(timeZone.trim());
        } catch (DateTimeException exception) {
            throw new DomainException(
                    "INVALID_TIME_ZONE",
                    "O fuso horário da MATRIZ é inválido.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private static DomainException loginConflict() {
        return new DomainException(
                "LOGIN_ALREADY_EXISTS",
                "Um dos logins informados já está em uso.",
                HttpStatus.CONFLICT);
    }

    private record InitialTeamMember(User user, Professional professional) {
        private ClinicRegistrationResponse.InitialAccountResponse toResponse() {
            return new ClinicRegistrationResponse.InitialAccountResponse(
                    user.getId(), professional.getId(), user.getRole(), professional.getType());
        }
    }
}
