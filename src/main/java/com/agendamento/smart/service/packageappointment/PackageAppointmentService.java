package com.agendamento.smart.service.packageappointment;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.dtos.appointment.AppointmentRequest;
import com.agendamento.smart.dtos.appointment.AppointmentResponse;
import com.agendamento.smart.dtos.packageappointment.*;
import com.agendamento.smart.model.packageappointment.PackageAppointmentContract;
import com.agendamento.smart.model.servicepackage.PackageWeekDay;
import com.agendamento.smart.model.servicepackage.ServicePackageDefinition;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.PackageAppointmentContractRepository;
import com.agendamento.smart.repository.ServicePackageRepository;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.service.appointment.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service @RequiredArgsConstructor
public class PackageAppointmentService {
    private final ServicePackageRepository packageRepository;
    private final PackageAppointmentContractRepository contractRepository;
    private final AppointmentService appointmentService;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public PackageAppointmentPreviewResponse preview(PackageAppointmentRequest request, User user) {
        ServicePackageDefinition definition = findPackage(request.packageId(), user);
        validatePatient(request.patientId(), definition);
        List<PackageAppointmentSession> sessions = sessions(definition, request);
        return response(definition, request, sessions);
    }

    @Transactional
    public PackageAppointmentConfirmationResponse confirm(PackageAppointmentRequest request, User user, String key, String fingerprint) {
        if (key == null || key.isBlank()) throw new DomainException("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key é obrigatório.", HttpStatus.BAD_REQUEST);
        UUID clinicId = user.getClinic().getId();
        var existing = contractRepository.findByClinicIdAndIdempotencyKey(clinicId, key);
        if (existing.isPresent()) return new PackageAppointmentConfirmationResponse(existing.get().getId(), List.of(), preview(request, user));
        PackageAppointmentPreviewResponse preview = preview(request, user);
        if (fingerprint != null && !fingerprint.equals(preview.fingerprint())) throw new DomainException("STALE_PACKAGE_PREVIEW", "A prévia do pacote está desatualizada.", HttpStatus.CONFLICT);
        ServicePackageDefinition definition = findPackage(request.packageId(), user);
        PackageAppointmentContract contract = new PackageAppointmentContract();
        contract.setClinic(user.getClinic()); contract.setPackageDefinition(definition); contract.setPatient(patientRepository.findByIdAndClinicIdWithUnits(request.patientId(), user.getClinic().getId()).orElseThrow(DomainException::tenantResourceNotFound));
        contract.setUnit(definition.getUnit()); contract.setService(definition.getService()); contract.setCreatedBy(user); contract.setTotalSessions(definition.getTotalSessions()); contract.setTotalPrice(definition.getTotalPrice()); contract.setRequestedStartDate(request.startDate()); contract.setAppointmentTime(request.time()); contract.setEffectiveFirstDate(preview.firstDate()); contract.setEffectiveLastDate(preview.lastDate()); contract.setEffectiveWeekDays(new LinkedHashSet<>(request.effectiveWeekDays())); contract.setIdempotencyKey(key);
        contractRepository.saveAndFlush(contract);
        BigDecimal price = definition.getTotalPrice().divide(BigDecimal.valueOf(definition.getTotalSessions()), 2, RoundingMode.HALF_UP);
        List<UUID> ids = new ArrayList<>(); int i = 0;
        for (PackageAppointmentSession session : preview.sessions()) {
            AppointmentResponse created = appointmentService.create(new AppointmentRequest(request.patientId(), definition.getUnit().getId(), definition.getService().getId(), request.professionalId(), session.startsAt(), definition.getService().getDurationMinutes(), price), user, key + "-" + (++i));
            ids.add(created.id());
        }
        return new PackageAppointmentConfirmationResponse(contract.getId(), ids, preview);
    }

    @Transactional(readOnly = true)
    public PackageAppointmentConfirmationResponse findById(UUID id, User user) {
        var c = contractRepository.findByIdAndClinicId(id, user.getClinic().getId()).orElseThrow(DomainException::tenantResourceNotFound);
        return new PackageAppointmentConfirmationResponse(c.getId(), List.of(), null);
    }

    private ServicePackageDefinition findPackage(UUID id, User user) { return packageRepository.findByIdAndClinicId(id, user.getClinic().getId()).filter(ServicePackageDefinition::isActive).orElseThrow(DomainException::tenantResourceNotFound); }
    private void validatePatient(UUID patientId, ServicePackageDefinition definition) { patientRepository.findByIdAndClinicIdWithUnits(patientId, definition.getClinic().getId()).orElseThrow(DomainException::tenantResourceNotFound); }
    private List<PackageAppointmentSession> sessions(ServicePackageDefinition definition, PackageAppointmentRequest request) { ZoneId zone = ZoneId.of(definition.getUnit().getTimeZone()); Set<DayOfWeek> days = request.effectiveWeekDays().stream().map(this::day).collect(java.util.stream.Collectors.toSet()); List<PackageAppointmentSession> result = new ArrayList<>(); LocalDate date = request.startDate(); while (result.size() < definition.getTotalSessions()) { if (days.contains(date.getDayOfWeek())) { ZonedDateTime z = ZonedDateTime.of(date, request.time(), zone); result.add(new PackageAppointmentSession(result.size()+1, date, request.time(), z.toInstant())); } date = date.plusDays(1); } return result; }
    private PackageAppointmentPreviewResponse response(ServicePackageDefinition d, PackageAppointmentRequest r, List<PackageAppointmentSession> s) { return new PackageAppointmentPreviewResponse(d.getId(), r.patientId(), d.getUnit().getId(), d.getTotalPrice(), s.get(0).date(), s.get(s.size()-1).date(), s, fingerprint(d,r,s)); }
    private String fingerprint(ServicePackageDefinition d, PackageAppointmentRequest r, List<PackageAppointmentSession> s) { try { var md=MessageDigest.getInstance("SHA-256"); return HexFormat.of().formatHex(md.digest((d.getId()+"|"+r.patientId()+"|"+r.startDate()+"|"+r.time()+"|"+r.effectiveWeekDays()+"|"+s.size()).getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
    private DayOfWeek day(PackageWeekDay d) { return DayOfWeek.valueOf(d.name()); }
}
