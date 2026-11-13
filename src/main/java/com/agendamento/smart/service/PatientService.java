package com.agendamento.smart.service;

import com.agendamento.smart.controller.exception.PatientAlreadyExistsException;
import com.agendamento.smart.controller.exception.DomainException;
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
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PatientService implements PatientServiceInt{

    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final UnitRepository unitRepository;
    private final PatientMapper patientMapper;
    private final UnitScopeService unitScopeService;
    @Override
    public PageResponseDTO<PatientResponseDTO> findAllPatient(Pageable pageable, Authentication authentication){
        User user = authenticatedUser(authentication);
        var pageEntity = unitScopeService.isAdmin(user)
                ? patientRepository.findAllByClinicId(user.getClinic().getId(), pageable)
                : patientRepository.findAllByClinicIdAndUnits_Id(
                        user.getClinic().getId(),
                        unitScopeService.requireSingleAllowedUnitId(user),
                        pageable);
        var pageDto = pageEntity.map(patientMapper::toDto);

        return PageResponseDTO.from(pageDto);
    }

    @Transactional
    public PatientResponseDTO save(PatientRequestDTO dto, Authentication authentication) {
        User principal = authenticatedUser(authentication);
        UUID clinicId = principal.getClinic().getId();
        if (dto.clinicId() != null && !dto.clinicId().equals(clinicId)) {
            throw new DomainException(
                    "CLINIC_ACCESS_DENIED",
                    "O clinicId informado não corresponde à clínica autenticada.",
                    HttpStatus.FORBIDDEN);
        }

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);

        LinkedHashSet<UUID> requestedUnitIds = new LinkedHashSet<>(dto.unitIds());
        List<ClinicUnit> units = unitRepository.findAllByClinicIdAndIdIn(clinicId, requestedUnitIds);
        if (units.size() != requestedUnitIds.size()) {
            throw DomainException.tenantResourceNotFound();
        }

        Patient patient = patientMapper.toEntity(dto);
        String namePatient = dto.name().trim().toUpperCase(Locale.ROOT);

        Optional<Patient> existsOpt = patientRepository.findByName(namePatient);

        existsOpt.ifPresent(p -> {
            throw new PatientAlreadyExistsException("Patient already exists");
        });


        patient.setName(namePatient);
        patient.setClinic(clinic);
        patient.setUnits(new LinkedHashSet<>(units));

        patient = patientRepository.saveAndFlush(patient);

        return patientMapper.toDto(patient);
    }

    private User authenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new DomainException(
                    "AUTHENTICATION_REQUIRED",
                    "Usuário não autenticado.",
                    HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

}
