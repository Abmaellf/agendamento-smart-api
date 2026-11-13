package com.agendamento.smart.service;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.dtos.user.RegisterDTO;
import com.agendamento.smart.model.professional.Professional;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.ProfessionalRepository;
import com.agendamento.smart.repository.UnitRepository;
import com.agendamento.smart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UnitRepository unitRepository;
    private final ProfessionalRepository professionalRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(UUID clinicId, RegisterDTO request, Authentication authentication) {
        User principal = authenticatedUser(authentication);
        if (principal.getRole() != UserRole.ADMIN) {
            throw new DomainException(
                    "USER_REGISTRATION_FORBIDDEN",
                    "Somente administradores podem cadastrar usuários.",
                    HttpStatus.FORBIDDEN);
        }
        if (!principal.getClinic().getId().equals(clinicId)) {
            throw new DomainException(
                    "CLINIC_ACCESS_DENIED",
                    "Não é permitido cadastrar usuário em outra clínica.",
                    HttpStatus.FORBIDDEN);
        }
        if (request.role() != UserRole.USER) {
            throw new DomainException(
                    "ADMIN_ROLE_NOT_ALLOWED",
                    "O cadastro posterior cria somente usuários USER.",
                    HttpStatus.FORBIDDEN);
        }

        String login = request.login().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByLoginIgnoreCase(login)) {
            throw new DomainException(
                    "LOGIN_ALREADY_EXISTS",
                    "O login informado já está em uso.",
                    HttpStatus.CONFLICT);
        }

        ClinicUnit unit = unitRepository.findByIdAndClinicId(request.unitId(), clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);

        User user = User.builder()
                .login(login)
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .clinic(principal.getClinic())
                .clinicUnit(unit)
                .build();
        user = userRepository.saveAndFlush(user);

        if (Boolean.TRUE.equals(request.isProfessional())) {
            createProfessional(user, unit, request.professional());
        }
    }

    private void createProfessional(
            User user,
            ClinicUnit matrix,
            RegisterDTO.ProfessionalData data
    ) {
        if (data == null) {
            throw new DomainException(
                    "PROFESSIONAL_DATA_REQUIRED",
                    "Os dados profissionais são obrigatórios.",
                    HttpStatus.BAD_REQUEST);
        }

        String availableWeekDays = data.availableWeekDays().stream()
                .sorted()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElseThrow(() -> new DomainException(
                        "PROFESSIONAL_DAYS_REQUIRED",
                        "Informe ao menos um dia disponível.",
                        HttpStatus.BAD_REQUEST));

        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setClinic(user.getClinic());
        professional.setName(data.name().trim());
        professional.setAvailableWeekDaysValue(availableWeekDays);
        professional.setUnit(matrix);
        professional.setUser(user);
        professional.setType(null);
        professional.setServices(new LinkedHashSet<>());
        professionalRepository.saveAndFlush(professional);
    }

    private User authenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new DomainException(
                    "AUTHENTICATION_REQUIRED",
                    "Usuário não autenticado.",
                    HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new DomainException(
                        "AUTHENTICATED_USER_NOT_FOUND",
                        "Usuário autenticado não encontrado.",
                        HttpStatus.UNAUTHORIZED));
    }
}
