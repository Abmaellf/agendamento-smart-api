package com.agendamento.smart.service.appointment;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.controller.appointment.AppointmentNoShowDeadlineException;
import com.agendamento.smart.dtos.appointment.AppointmentRequest;
import com.agendamento.smart.dtos.appointment.AppointmentResponse;
import com.agendamento.smart.dtos.appointment.AppointmentResponse.ReferenceResponse;
import com.agendamento.smart.dtos.appointment.StartCurrentHourRequest;
import com.agendamento.smart.dtos.appointment.StartCurrentHourResponse;
import com.agendamento.smart.model.appointment.Appointment;
import com.agendamento.smart.model.appointment.AppointmentStatus;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.clinic.StatusClinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.professional.Professional;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.unit.StatusUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.AppointmentRepository;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.ProfessionalRepository;
import com.agendamento.smart.repository.ServiceOfferingRepository;
import com.agendamento.smart.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final DateTimeFormatter INSTANT_WITH_MILLIS =
            new DateTimeFormatterBuilder().appendInstant(3).toFormatter();
    private static final Duration NO_SHOW_GRACE_PERIOD = Duration.ofMinutes(10);

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final UnitRepository unitRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final Clock clock;

    @Transactional
    public AppointmentResponse create(AppointmentRequest request, User user, String idempotencyKey) {
        UUID clinicId = user.getClinic().getId();
        Clinic clinic = clinicRepository.findByIdForUpdate(clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        requireClinicActiveForCreation(clinic);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Appointment existing = appointmentRepository
                    .findByClinicIdAndIdempotencyKey(clinicId, idempotencyKey)
                    .orElse(null);
            if (existing != null) return toResponse(existing);
        }

        ClinicUnit unit = unitRepository.findByIdAndClinicId(request.unitId(), clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        requireUserUnit(user, unit.getId());
        requireUnitActiveForCreation(unit);
        Patient patient = patientRepository.findByIdAndClinicIdWithUnits(request.patientId(), clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        requirePatientInUnit(patient, unit.getId());
        ServiceOffering service = serviceRepository.findByIdAndClinicId(request.serviceId(), clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        Professional professional = resolveProfessional(
                request.professionalId(), clinicId, unit.getId(), service.getId());

        if (!request.startsAt().isAfter(clock.instant())) {
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
        List<Appointment> active = appointmentRepository.findAllByClinicIdAndStartsAtIsNotNull(clinicId).stream()
                .filter(this::isActive)
                .toList();

        boolean patientConflict = active.stream().anyMatch(existing ->
                existing.getPatient().getId().equals(patient.getId())
                        && overlaps(existing, request.startsAt(), requestedEnd));
        if (patientConflict) {
            throw new DomainException(
                    "PATIENT_APPOINTMENT_CONFLICT",
                    "O paciente já possui outro agendamento no intervalo.",
                    HttpStatus.CONFLICT);
        }

        boolean individualService = "Atendimento individual".equalsIgnoreCase(service.getName());
        if (professional != null && individualService) {
            boolean professionalConflict = active.stream().anyMatch(existing ->
                    existing.getProfessional() != null
                            && existing.getProfessional().getId().equals(professional.getId())
                            && overlaps(existing, request.startsAt(), requestedEnd));
            if (professionalConflict) {
                throw new DomainException(
                        "PROFESSIONAL_APPOINTMENT_CONFLICT",
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
        Appointment appointment = new Appointment();
        appointment.setClinic(clinic);
        appointment.setUnit(unit);
        appointment.setPatient(patient);
        appointment.setService(service);
        appointment.setProfessional(professional);
        appointment.setStartsAt(request.startsAt());
        appointment.setTimeZone(unit.getTimeZone());
        appointment.setDurationMinutes(request.durationMinutes());
        appointment.setPrice(requestedPrice);
        appointment.setStatus(AppointmentStatus.AGENDADO);
        appointment.setCreatedBy(user);
        appointment.setIdempotencyKey(normalizeIdempotencyKey(idempotencyKey));
        appointment.setPathology(List.of());
        appointment.setAppointmentDate(localStart);
        appointment.setHours(localStart.toLocalTime());
        appointment.setVariant("primary");

        return toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    @Transactional
    public AppointmentResponse findById(UUID id, User user) {
        Appointment appointment = appointmentRepository.findByIdAndClinicId(id, user.getClinic().getId())
                .orElseThrow(DomainException::tenantResourceNotFound);
        requireUserUnit(user, appointment.getUnit().getId());
        requireClinicReadable(appointment.getClinic());
        requireUnitReadable(appointment.getUnit());
        markAsNoShowIfOverdue(appointment, clock.instant());
        return toResponse(appointment);
    }

    @Transactional
    public List<AppointmentResponse> findAll(User user, UUID unitId, Instant from, Instant to) {
        UUID clinicId = user.getClinic().getId();
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        requireClinicReadable(clinic);
        requireValidRange(from, to);
        reconcileOverdueAppointments(clinicId, clock.instant());

        List<Appointment> appointments;
        if (user.getRole() != UserRole.ADMIN) {
            UUID allowedUnitId = user.getClinicUnit().getId();
            if (unitId != null && !unitId.equals(allowedUnitId)) throw DomainException.tenantResourceNotFound();
            ClinicUnit unit = unitRepository.findByIdAndClinicId(allowedUnitId, clinicId)
                    .orElseThrow(DomainException::tenantResourceNotFound);
            requireUnitReadable(unit);
            appointments = appointmentRepository.findAllByClinicIdAndUnitIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(clinicId, allowedUnitId, from, to);
        } else if (unitId == null) {
            appointments = appointmentRepository.findReadableByClinicIdAndStartsAtBetween(
                    clinicId, StatusUnit.INATIVO, from, to);
        } else {
            ClinicUnit unit = unitRepository.findByIdAndClinicId(unitId, clinicId)
                    .orElseThrow(DomainException::tenantResourceNotFound);
            requireUnitReadable(unit);
            appointments = appointmentRepository
                    .findAllByClinicIdAndUnitIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                            clinicId, unitId, from, to);
        }

        return appointments
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(noRollbackFor = AppointmentNoShowDeadlineException.class)
    public AppointmentResponse updateStatus(UUID id, AppointmentStatus requestedStatus, User user) {
        if (requestedStatus != AppointmentStatus.ATENDENDO
                && requestedStatus != AppointmentStatus.CANCELADO) {
            throw invalidStatusTransition();
        }

        Appointment appointment = appointmentRepository
                .findByIdAndClinicIdForUpdate(id, user.getClinic().getId())
                .orElseThrow(DomainException::tenantResourceNotFound);
        requireUserUnit(user, appointment.getUnit().getId());
        requireClinicReadable(appointment.getClinic());
        requireUnitReadable(appointment.getUnit());

        if (appointment.getStatus() != AppointmentStatus.AGENDADO) {
            throw invalidStatusTransition();
        }
        if (isNoShowDue(appointment, clock.instant())) {
            appointment.setStatus(AppointmentStatus.FALTA);
            appointmentRepository.saveAndFlush(appointment);
            throw new AppointmentNoShowDeadlineException();
        }

        appointment.setStatus(requestedStatus);
        return toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    @Transactional
    public StartCurrentHourResponse startCurrentHour(StartCurrentHourRequest request, User user) {
        UUID clinicId = user.getClinic().getId();
        Clinic clinic = clinicRepository.findByIdForUpdate(clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        requireClinicReadable(clinic);

        ClinicUnit unit = unitRepository.findByIdAndClinicId(request.unitId(), clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        requireUserUnit(user, unit.getId());
        requireUnitReadable(unit);

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(unit.getTimeZone());
        } catch (DateTimeException exception) {
            throw new DomainException(
                    "UNIT_TIME_ZONE_INVALID",
                    "O fuso horário da unidade é inválido.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Instant evaluatedAt = clock.instant();
        ZonedDateTime currentHour = evaluatedAt.atZone(zoneId).truncatedTo(ChronoUnit.HOURS);
        ZonedDateTime requestedHour = request.hourStart().atZone(zoneId);
        if (requestedHour.getMinute() != 0
                || requestedHour.getSecond() != 0
                || requestedHour.getNano() != 0) {
            throw new DomainException(
                    "INVALID_APPOINTMENT_HOUR",
                    "A hora deve representar o início de uma hora local.",
                    HttpStatus.BAD_REQUEST);
        }
        if (!requestedHour.toLocalDate().equals(currentHour.toLocalDate())
                || requestedHour.getHour() != currentHour.getHour()) {
            throw new DomainException(
                    "APPOINTMENT_HOUR_NOT_CURRENT",
                    "A faixa solicitada não é a hora corrente da unidade.",
                    HttpStatus.CONFLICT);
        }

        Instant hourStart = currentHour.toInstant();
        Instant hourEnd = currentHour.plusHours(1).toInstant();
        List<Appointment> matched = appointmentRepository.findForCurrentHourUpdate(
                clinicId, unit.getId(), hourStart, hourEnd);
        Instant postLockEvaluatedAt = clock.instant();
        ZonedDateTime postLockCurrentHour = postLockEvaluatedAt.atZone(zoneId)
                .truncatedTo(ChronoUnit.HOURS);
        if (!postLockCurrentHour.toLocalDate().equals(currentHour.toLocalDate())
                || postLockCurrentHour.getHour() != currentHour.getHour()) {
            throw new DomainException(
                    "APPOINTMENT_HOUR_NOT_CURRENT",
                    "A faixa solicitada deixou de ser a hora corrente da unidade.",
                    HttpStatus.CONFLICT);
        }
        evaluatedAt = postLockEvaluatedAt;
        Map<AppointmentStatus, Integer> skippedByStatus = new LinkedHashMap<>();
        skippedByStatus.put(AppointmentStatus.ATENDENDO, 0);
        skippedByStatus.put(AppointmentStatus.CANCELADO, 0);
        skippedByStatus.put(AppointmentStatus.FINALIZADO, 0);
        List<Appointment> eligible = matched.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.AGENDADO
                        || appointment.getStatus() == AppointmentStatus.FALTA)
                .toList();

        validateCollectiveConflicts(clinicId, eligible);

        for (Appointment appointment : eligible) {
            appointment.setStatus(AppointmentStatus.ATENDENDO);
        }
        if (!eligible.isEmpty()) {
            appointmentRepository.saveAllAndFlush(eligible);
        }
        matched.stream()
                .filter(appointment -> !eligible.contains(appointment))
                .forEach(appointment -> skippedByStatus.merge(
                        appointment.getStatus(), 1, Integer::sum));

        return new StartCurrentHourResponse(
                unit.getId(),
                unit.getTimeZone(),
                hourStart,
                hourEnd,
                evaluatedAt,
                AppointmentStatus.ATENDENDO,
                matched.size(),
                eligible.size(),
                matched.size() - eligible.size(),
                skippedByStatus);
    }

    private void validateCollectiveConflicts(UUID clinicId, List<Appointment> eligible) {
        if (eligible.isEmpty()) return;
        List<Appointment> all = appointmentRepository.findAllByClinicIdAndStartsAtIsNotNull(clinicId);
        List<Appointment> projectedActive = new ArrayList<>(all.stream()
                .filter(appointment -> isActive(appointment) || eligible.contains(appointment))
                .toList());

        for (Appointment candidate : eligible) {
            Instant candidateEnd = candidate.getStartsAt()
                    .plusSeconds(candidate.getDurationMinutes() * 60L);
            for (Appointment existing : projectedActive) {
                if (existing.getId().equals(candidate.getId()) || !overlaps(existing, candidate.getStartsAt(), candidateEnd)) {
                    continue;
                }
                if (existing.getPatient().getId().equals(candidate.getPatient().getId())) {
                    throw new DomainException(
                            "PATIENT_APPOINTMENT_CONFLICT",
                            "O paciente já possui outro agendamento no intervalo.",
                            HttpStatus.CONFLICT);
                }
                boolean individualService = candidate.getService() != null
                        && "Atendimento individual".equalsIgnoreCase(candidate.getService().getName());
                if (individualService && candidate.getProfessional() != null
                        && existing.getProfessional() != null
                        && existing.getProfessional().getId().equals(candidate.getProfessional().getId())) {
                    throw new DomainException(
                            "PROFESSIONAL_APPOINTMENT_CONFLICT",
                            "O profissional já possui outro agendamento no intervalo.",
                            HttpStatus.CONFLICT);
                }
            }

            if (candidate.getService() != null) {
                long occupancy = projectedActive.stream()
                        .filter(existing -> existing.getService() != null
                                && existing.getService().getId().equals(candidate.getService().getId())
                                && overlaps(existing, candidate.getStartsAt(), candidateEnd))
                        .count();
                if (occupancy > candidate.getService().getCapacity()) {
                    throw new DomainException(
                            "SERVICE_CAPACITY_EXCEEDED",
                            "A capacidade do serviço está esgotada.",
                            HttpStatus.CONFLICT);
                }
            }
        }
    }

    @Transactional
    public int reconcileOverdueAppointments(Instant now) {
        Instant cutoff = noShowCutoff(now);
        return appointmentRepository.markOverdueAsNoShow(
                AppointmentStatus.AGENDADO,
                AppointmentStatus.FALTA,
                cutoff);
    }

    @Transactional
    public int reconcileOverdueAppointments(UUID clinicId, Instant now) {
        Instant cutoff = noShowCutoff(now);
        return appointmentRepository.markClinicOverdueAsNoShow(
                clinicId,
                AppointmentStatus.AGENDADO,
                AppointmentStatus.FALTA,
                cutoff);
    }

    @Transactional(readOnly = true)
    public long countOverdueAppointments(Instant now) {
        return appointmentRepository.countByStatusAndStartsAtLessThanEqual(
                AppointmentStatus.AGENDADO,
                noShowCutoff(now));
    }

    private void markAsNoShowIfOverdue(Appointment appointment, Instant now) {
        if (isNoShowDue(appointment, now)) {
            appointment.setStatus(AppointmentStatus.FALTA);
            appointmentRepository.saveAndFlush(appointment);
        }
    }

    private boolean isNoShowDue(Appointment appointment, Instant now) {
        return appointment.getStatus() == AppointmentStatus.AGENDADO
                && appointment.getStartsAt() != null
                && !appointment.getStartsAt().plus(NO_SHOW_GRACE_PERIOD).isAfter(now);
    }

    private Instant noShowCutoff(Instant now) {
        return now.minus(NO_SHOW_GRACE_PERIOD);
    }

    private DomainException invalidStatusTransition() {
        return new DomainException(
                "APPOINTMENT_STATUS_TRANSITION_INVALID",
                "O agendamento não pode assumir o status solicitado.",
                HttpStatus.CONFLICT);
    }

    private Professional resolveProfessional(
            UUID professionalId,
            UUID clinicId,
            UUID unitId,
            UUID serviceId) {
        if (professionalId == null) return null;
        Professional professional = professionalRepository.findByIdAndClinicId(professionalId, clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        if (!professional.getUnit().getId().equals(unitId)) {
            throw new DomainException(
                    "PROFESSIONAL_UNIT_MISMATCH",
                    "O profissional não pertence à unidade do agendamento.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!professional.offers(serviceId)) throw DomainException.tenantResourceNotFound();
        return professional;
    }

    private void requireClinicActiveForCreation(Clinic clinic) {
        if (clinic.getStatus() != StatusClinic.ATIVO) {
            throw new DomainException(
                    "CLINIC_APPOINTMENTS_DISABLED",
                    "A clínica não permite novos agendamentos no status atual.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void requireUnitActiveForCreation(ClinicUnit unit) {
        if (unit.getStatus() != StatusUnit.ATIVO) {
            throw new DomainException(
                    "UNIT_APPOINTMENTS_DISABLED",
                    "A unidade não permite novos agendamentos no status atual.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void requirePatientInUnit(Patient patient, UUID unitId) {
        boolean linked = patient.getUnits().stream()
                .anyMatch(unit -> unit.getId().equals(unitId));
        if (!linked) {
            throw new DomainException(
                    "PATIENT_UNIT_MISMATCH",
                    "O paciente não pertence à unidade do agendamento.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void requireUserUnit(User user, UUID unitId) {
        if (user.getRole() != UserRole.ADMIN && !user.getClinicUnit().getId().equals(unitId)) {
            throw DomainException.tenantResourceNotFound();
        }
    }

    private void requireClinicReadable(Clinic clinic) {
        if (clinic.getStatus() == StatusClinic.INATIVO) {
            throw new DomainException(
                    "CLINIC_APPOINTMENTS_READ_DISABLED",
                    "A clínica não permite consultar agendamentos no status atual.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void requireUnitReadable(ClinicUnit unit) {
        if (unit.getStatus() == StatusUnit.INATIVO) {
            throw new DomainException(
                    "UNIT_APPOINTMENTS_READ_DISABLED",
                    "A unidade não permite consultar agendamentos no status atual.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void requireValidRange(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new DomainException(
                    "INVALID_APPOINTMENT_RANGE",
                    "O início do intervalo deve ser anterior ao fim.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isActive(Appointment appointment) {
        return appointment.getStatus() == AppointmentStatus.AGENDADO
                || appointment.getStatus() == AppointmentStatus.ATENDENDO;
    }

    private boolean overlaps(Appointment existing, Instant requestedStart, Instant requestedEnd) {
        Instant existingEnd = existing.getStartsAt().plusSeconds(existing.getDurationMinutes() * 60L);
        return existing.getStartsAt().isBefore(requestedEnd) && existingEnd.isAfter(requestedStart);
    }

    private String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) return null;
        return value.length() <= 120 ? value : value.substring(0, 120);
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        ReferenceResponse professional = appointment.getProfessional() == null
                ? null
                : new ReferenceResponse(
                        appointment.getProfessional().getId(), appointment.getProfessional().getName());
        ReferenceResponse service = appointment.getService() == null
                ? null
                : new ReferenceResponse(appointment.getService().getId(), appointment.getService().getName());
        UUID tenantId = appointment.getUnit() == null ? null : appointment.getUnit().getTenantId();

        return new AppointmentResponse(
                appointment.getId(),
                tenantId,
                appointment.getUnit() == null ? null : appointment.getUnit().getId(),
                new ReferenceResponse(appointment.getPatient().getId(), appointment.getPatient().getName()),
                service,
                professional,
                appointment.getStartsAt() == null
                        ? null
                        : INSTANT_WITH_MILLIS.format(appointment.getStartsAt()),
                appointment.getTimeZone(),
                appointment.getDurationMinutes(),
                appointment.getPrice() == null
                        ? null
                        : appointment.getPrice().setScale(2, RoundingMode.UNNECESSARY),
                appointment.getStatus(),
                appointment.getCreatedBy() == null ? null : appointment.getCreatedBy().getId(),
                appointment.getCreatedAt(),
                appointment.getPatient().getId(),
                appointment.getPathology(),
                appointment.getAppointmentDate(),
                appointment.getHours(),
                appointment.getVariant(),
                appointment.getUpdatedAt());
    }
}
