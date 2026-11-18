package com.agendamento.smart.service;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.controller.exception.PatientAlreadyExistsException;
import com.agendamento.smart.dtos.PageResponseDTO;
import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.mapper.PatientMapper;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService implements PatientServiceInt {

    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final UnitRepository unitRepository;
    private final PatientMapper patientMapper;
    private final UnitScopeService unitScopeService;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<PatientResponseDTO> findAllPatient(
            Pageable pageable,
            String name,
            Authentication authentication) {
        User user = authenticatedUser(authentication);
        UUID clinicId = authenticatedClinicId(user);
        String normalizedName = normalizeNameFilter(name);

        Page<Patient> patientPage = unitScopeService.isAdmin(user)
                ? patientRepository.findPageByClinicIdAndName(
                clinicId,
                normalizedName,
                pageable)
                : patientRepository.findPageByClinicIdAndUnitIdAndName(
                clinicId,
                unitScopeService.requireSingleAllowedUnitId(user),
                normalizedName,
                pageable);

        return PageResponseDTO.from(patientPage.map(patientMapper::toDto));
    }

    @Override
    @Transactional
    public PatientResponseDTO save(
            PatientRequestDTO dto,
            Authentication authentication) {
        User principal = authenticatedUser(authentication);
        UUID clinicId = authenticatedClinicId(principal);
        LinkedHashSet<UUID> requestedUnitIds = requestedUnitIds(dto);

        validateClinicId(dto.clinicId(), clinicId);
        validateUserUnitScope(principal, requestedUnitIds);

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);

        List<ClinicUnit> units = unitRepository.findAllByClinicIdAndIdIn(
                clinicId,
                requestedUnitIds);
        if (units.size() != requestedUnitIds.size()) {
            throw DomainException.tenantResourceNotFound();
        }

        String normalizedName = normalizePatientName(dto.name());
        if (patientRepository.findByName(normalizedName).isPresent()) {
            throw new PatientAlreadyExistsException(
                    "Já existe um paciente com esse nome.");
        }

        Patient patient = patientMapper.toEntity(dto);
        patient.setName(normalizedName);
        patient.setClinic(clinic);
        patient.setUnits(new LinkedHashSet<>(units));

        Patient persisted = patientRepository.saveAndFlush(patient);
        return patientMapper.toDto(persisted);
    }

    private User authenticatedUser(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new DomainException(
                    "AUTHENTICATION_REQUIRED",
                    "Usuário não autenticado.",
                    HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    private UUID authenticatedClinicId(User user) {
        if (user.getClinic() == null || user.getClinic().getId() == null) {
            throw DomainException.tenantResourceNotFound();
        }
        return user.getClinic().getId();
    }

    private LinkedHashSet<UUID> requestedUnitIds(PatientRequestDTO dto) {
        if (dto.unitIds() == null
                || dto.unitIds().isEmpty()
                || dto.unitIds().stream().anyMatch(Objects::isNull)) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "Informe pelo menos uma unidade válida.",
                    HttpStatus.BAD_REQUEST);
        }

        return new LinkedHashSet<>(dto.unitIds());
    }

    private void validateClinicId(
            UUID requestedClinicId,
            UUID authenticatedClinicId) {
        if (requestedClinicId != null
                && !requestedClinicId.equals(authenticatedClinicId)) {
            throw new DomainException(
                    "CLINIC_ACCESS_DENIED",
                    "O clinicId informado não corresponde à clínica autenticada.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void validateUserUnitScope(
            User principal,
            LinkedHashSet<UUID> requestedUnitIds) {
        if (unitScopeService.isAdmin(principal)) {
            return;
        }

        UUID activeUnitId = unitScopeService
                .requireSingleAllowedUnitId(principal);
        LinkedHashSet<UUID> allowedUnitIds =
                new LinkedHashSet<>(List.of(activeUnitId));

        if (!requestedUnitIds.equals(allowedUnitIds)) {
            throw new DomainException(
                    "UNIT_ACCESS_DENIED",
                    "O usuário só pode cadastrar paciente na unidade associada.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private String normalizePatientName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "Informe o nome do paciente.",
                    HttpStatus.BAD_REQUEST);
        }
        return name.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNameFilter(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }
}
