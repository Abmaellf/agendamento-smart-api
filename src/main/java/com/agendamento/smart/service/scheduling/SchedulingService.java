package com.agendamento.smart.service.scheduling;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.dtos.agendamento.SchedulingRequest;
import com.agendamento.smart.dtos.agendamento.SchedulingResponse;
import com.agendamento.smart.dtos.agendamento.SchedulingResponse.ReferenceResponse;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.professional.Professional;
import com.agendamento.smart.model.scheduling.Scheduling;
import com.agendamento.smart.model.scheduling.StatusScheduling;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.ProfessionalRepository;
import com.agendamento.smart.repository.SchedulingRepository;
import com.agendamento.smart.repository.ServiceOfferingRepository;
import com.agendamento.smart.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private static final DateTimeFormatter INSTANT_WITH_MILLIS =
            new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

    private final SchedulingRepository schedulingRepository;
    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final UnitRepository unitRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;

    @Transactional
    public SchedulingResponse create(SchedulingRequest request, User user, String idempotencyKey) {
        UUID clinicId = user.getClinic().getId();
        Clinic clinic = clinicRepository.findByIdForUpdate(clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Scheduling existing = schedulingRepository
                    .findByClinicIdAndIdempotencyKey(clinicId, idempotencyKey)
                    .orElse(null);
            if (existing != null) return toResponse(existing);
        }

        Patient patient = patientRepository.findByIdAndClinicId(request.patientId(), clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        ClinicUnit unit = unitRepository.findByIdAndClinicId(request.unitId(), clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        ServiceOffering service = serviceRepository.findByIdAndClinicId(request.serviceId(), clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        Professional professional = resolveProfessional(request.professionalId(), clinicId, service.getId());

        if (!request.startsAt().isAfter(Instant.now())) {
            throw new DomainException(
                    "APPOINTMENT_IN_THE_PAST",
                    "Não é possível criar agendamento no passado.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }



        BigDecimal requestedPrice = request.price().setScale(2, RoundingMode.UNNECESSARY);
        boolean overridesDefaults =
                !request.durationMinutes().equals(service.getDurationMinutes())
                        || requestedPrice.compareTo(service.getPrice()) != 0;
        if (user.getRole() != UserRole.ADMIN && overridesDefaults) {
            throw new DomainException(
                    "APPOINTMENT_OVERRIDE_FORBIDDEN",
                    "Somente administradores podem alterar os padrões do serviço.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        ZoneId zoneId = ZoneId.of(unit.getTimeZone());
        if (professional != null
                && !professional.availableWeekDays().contains(request.startsAt().atZone(zoneId).getDayOfWeek().getValue())) {
            throw new DomainException(
                    "PROFESSIONAL_UNAVAILABLE_DAY",
                    "Profissional indisponível no dia solicitado.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Instant requestedEnd = request.startsAt().plusSeconds(request.durationMinutes() * 60L);
        List<Scheduling> active = schedulingRepository.findAllByClinicIdAndStartsAtIsNotNull(clinicId).stream()
                .filter(this::isActive)
                .toList();

        boolean patientConflict = active.stream().anyMatch(existing ->
                existing.getPatient().getId().equals(patient.getId())
                        && overlaps(existing, request.startsAt(), requestedEnd));
        if (patientConflict) {
            throw new DomainException(
                    "PATIENT_SCHEDULE_CONFLICT",
                    "O paciente já possui outro agendamento no intervalo.",
                    HttpStatus.CONFLICT);
        }

        if (professional != null) {
            boolean professionalConflict = active.stream().anyMatch(existing ->
                    existing.getProfessional() != null
                            && existing.getProfessional().getId().equals(professional.getId())
                            && overlaps(existing, request.startsAt(), requestedEnd));
            if (professionalConflict) {
                throw new DomainException(
                        "PROFESSIONAL_SCHEDULE_CONFLICT",
                        "O profissional já possui outro agendamento no intervalo.",
                        HttpStatus.CONFLICT);
            }
        }

        long occupancy = active.stream()
                .filter(existing -> existing.getService() != null)
                .filter(existing -> existing.getService().getId().equals(service.getId()))
                .filter(existing -> overlaps(existing, request.startsAt(), requestedEnd))
                .count();
        if (occupancy >= service.getCapacity()) {
            throw new DomainException(
                    "SERVICE_CAPACITY_EXCEEDED",
                    "A capacidade do serviço está esgotada.",
                    HttpStatus.CONFLICT);
        }

        LocalDateTime localStart = LocalDateTime.ofInstant(request.startsAt(), zoneId);
        Scheduling scheduling = new Scheduling();
        scheduling.setClinic(clinic);
        scheduling.setUnit(unit);
        scheduling.setPatient(patient);
        scheduling.setService(service);
        scheduling.setProfessional(professional);
        scheduling.setStartsAt(request.startsAt());
        scheduling.setTimeZone(unit.getTimeZone());
        scheduling.setDurationMinutes(request.durationMinutes());
        scheduling.setPrice(requestedPrice);
        scheduling.setStatus(StatusScheduling.AGENDADO);
        scheduling.setCreatedBy(user);
        scheduling.setIdempotencyKey(normalizeIdempotencyKey(idempotencyKey));
        scheduling.setPathology(List.of());
        scheduling.setDateScheduling(localStart);
        scheduling.setHours(localStart.toLocalTime());
        scheduling.setVariant("primary");

        return toResponse(schedulingRepository.saveAndFlush(scheduling));
    }

    @Transactional(readOnly = true)
    public SchedulingResponse findById(UUID id, User user) {
        Scheduling scheduling = schedulingRepository.findByIdAndClinicId(id, user.getClinic().getId())
                .orElseThrow(DomainException::tenantResourceNotFound);
        return toResponse(scheduling);
    }

    @Transactional(readOnly = true)
    public List<SchedulingResponse> findAll(User user, UUID unitId, Instant from, Instant to) {
        UUID clinicId = user.getClinic().getId();
        unitRepository.findByIdAndClinicId(unitId, clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        return schedulingRepository
                .findAllByClinicIdAndUnitIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        clinicId, unitId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Professional resolveProfessional(UUID professionalId, UUID clinicId, UUID serviceId) {
        if (professionalId == null) return null;
        Professional professional = professionalRepository.findByIdAndClinicId(professionalId, clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        if (!professional.offers(serviceId)) throw DomainException.tenantResourceNotFound();
        return professional;
    }

    private boolean isActive(Scheduling scheduling) {
        return scheduling.getStatus() == StatusScheduling.AGENDADO
                || scheduling.getStatus() == StatusScheduling.ATENDENDO;
    }

    private boolean overlaps(Scheduling existing, Instant requestedStart, Instant requestedEnd) {
        Instant existingEnd = existing.getStartsAt().plusSeconds(existing.getDurationMinutes() * 60L);
        return existing.getStartsAt().isBefore(requestedEnd) && existingEnd.isAfter(requestedStart);
    }

    private String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) return null;
        return value.length() <= 120 ? value : value.substring(0, 120);
    }

    private SchedulingResponse toResponse(Scheduling scheduling) {
        ReferenceResponse professional = scheduling.getProfessional() == null
                ? null
                : new ReferenceResponse(
                        scheduling.getProfessional().getId(), scheduling.getProfessional().getName());
        ReferenceResponse service = scheduling.getService() == null
                ? null
                : new ReferenceResponse(scheduling.getService().getId(), scheduling.getService().getName());
        UUID tenantId = scheduling.getUnit() == null ? null : scheduling.getUnit().getTenantId();

        return new SchedulingResponse(
                scheduling.getId(),
                tenantId,
                scheduling.getUnit() == null ? null : scheduling.getUnit().getId(),
                new ReferenceResponse(scheduling.getPatient().getId(), scheduling.getPatient().getName()),
                service,
                professional,
                scheduling.getStartsAt() == null
                        ? null
                        : INSTANT_WITH_MILLIS.format(scheduling.getStartsAt()),
                scheduling.getTimeZone(),
                scheduling.getDurationMinutes(),
                scheduling.getPrice() == null
                        ? null
                        : scheduling.getPrice().setScale(2, RoundingMode.UNNECESSARY),
                scheduling.getStatus(),
                scheduling.getCreatedBy() == null ? null : scheduling.getCreatedBy().getId(),
                scheduling.getCreatedAt(),
                scheduling.getPatient().getId(),
                scheduling.getPathology(),
                scheduling.getDateScheduling(),
                scheduling.getHours(),
                scheduling.getVariant(),
                scheduling.getUpdatedAt());
    }
}
