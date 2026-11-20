package com.agendamento.smart.service.appointment;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.dtos.appointment.StartCurrentHourRequest;
import com.agendamento.smart.dtos.appointment.StartCurrentHourResponse;
import com.agendamento.smart.model.appointment.Appointment;
import com.agendamento.smart.model.appointment.AppointmentStatus;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.unit.StatusUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.model.clinic.StatusClinic;
import com.agendamento.smart.repository.AppointmentRepository;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.ProfessionalRepository;
import com.agendamento.smart.repository.ServiceOfferingRepository;
import com.agendamento.smart.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceStartCurrentHourTest {

    private static final Instant NOW = Instant.parse("2026-08-13T16:30:00Z");

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private ClinicRepository clinicRepository;
    @Mock private UnitRepository unitRepository;
    @Mock private ServiceOfferingRepository serviceRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private Clock clock;
    @InjectMocks private AppointmentService service;

    @Test
    void startsScheduledAndNoShowAppointmentsInTheCurrentUnitHour() {
        UUID clinicId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        Clinic clinic = clinic(clinicId);
        ClinicUnit unit = unit(unitId, clinic);
        User user = User.builder().clinic(clinic).clinicUnit(unit).role(UserRole.ADMIN).build();
        Appointment scheduled = appointment(clinic, unit, AppointmentStatus.AGENDADO, "2026-08-13T16:05:00Z");
        Appointment noShow = appointment(clinic, unit, AppointmentStatus.FALTA, "2026-08-13T16:20:00Z");
        Appointment cancelled = appointment(clinic, unit, AppointmentStatus.CANCELADO, "2026-08-13T16:25:00Z");

        when(clock.instant()).thenReturn(NOW);
        when(clinicRepository.findByIdForUpdate(clinicId)).thenReturn(Optional.of(clinic));
        when(unitRepository.findByIdAndClinicId(unitId, clinicId)).thenReturn(Optional.of(unit));
        when(appointmentRepository.findForCurrentHourUpdate(
                clinicId, unitId, Instant.parse("2026-08-13T16:00:00Z"), Instant.parse("2026-08-13T17:00:00Z")))
                .thenReturn(List.of(scheduled, noShow, cancelled));
        when(appointmentRepository.findAllByClinicIdAndStartsAtIsNotNull(clinicId))
                .thenReturn(List.of(scheduled, noShow, cancelled));

        StartCurrentHourResponse response = service.startCurrentHour(
                new StartCurrentHourRequest(unitId, Instant.parse("2026-08-13T16:00:00Z")), user);

        assertThat(response.matchedCount()).isEqualTo(3);
        assertThat(response.updatedCount()).isEqualTo(2);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.skippedByStatus()).containsEntry(AppointmentStatus.CANCELADO, 1);
        assertThat(response.skippedByStatus())
                .containsEntry(AppointmentStatus.ATENDENDO, 0)
                .containsEntry(AppointmentStatus.FINALIZADO, 0);
        assertThat(scheduled.getStatus()).isEqualTo(AppointmentStatus.ATENDENDO);
        assertThat(noShow.getStatus()).isEqualTo(AppointmentStatus.ATENDENDO);
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELADO);
        verify(appointmentRepository).saveAllAndFlush(List.of(scheduled, noShow));

        StartCurrentHourResponse repeated = service.startCurrentHour(
                new StartCurrentHourRequest(unitId, Instant.parse("2026-08-13T16:00:00Z")), user);
        assertThat(repeated.updatedCount()).isZero();
        assertThat(repeated.skippedCount()).isEqualTo(3);
    }

    @Test
    void rejectsAnHourThatIsNoLongerCurrent() {
        UUID clinicId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        Clinic clinic = clinic(clinicId);
        ClinicUnit unit = unit(unitId, clinic);
        User user = User.builder().clinic(clinic).clinicUnit(unit).role(UserRole.ADMIN).build();

        when(clock.instant()).thenReturn(NOW);
        when(clinicRepository.findByIdForUpdate(clinicId)).thenReturn(Optional.of(clinic));
        when(unitRepository.findByIdAndClinicId(unitId, clinicId)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> service.startCurrentHour(
                new StartCurrentHourRequest(unitId, Instant.parse("2026-08-13T15:00:00Z")), user))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("APPOINTMENT_HOUR_NOT_CURRENT");
                    assertThat(exception.getStatus().value()).isEqualTo(409);
                });
        verify(appointmentRepository, never()).findForCurrentHourUpdate(any(), any(), any(), any());
    }

    @Test
    void rechecksTheClockAfterTheSelectionLockBeforeUpdating() {
        UUID clinicId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        Clinic clinic = clinic(clinicId);
        ClinicUnit unit = unit(unitId, clinic);
        User user = User.builder().clinic(clinic).clinicUnit(unit).role(UserRole.ADMIN).build();
        Appointment appointment = appointment(clinic, unit, AppointmentStatus.AGENDADO, "2026-08-13T16:05:00Z");

        when(clock.instant()).thenReturn(NOW, Instant.parse("2026-08-13T17:00:01Z"));
        when(clinicRepository.findByIdForUpdate(clinicId)).thenReturn(Optional.of(clinic));
        when(unitRepository.findByIdAndClinicId(unitId, clinicId)).thenReturn(Optional.of(unit));
        when(appointmentRepository.findForCurrentHourUpdate(
                clinicId, unitId, Instant.parse("2026-08-13T16:00:00Z"), Instant.parse("2026-08-13T17:00:00Z")))
                .thenReturn(List.of(appointment));

        assertThatThrownBy(() -> service.startCurrentHour(
                new StartCurrentHourRequest(unitId, Instant.parse("2026-08-13T16:00:00Z")), user))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("APPOINTMENT_HOUR_NOT_CURRENT"));
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.AGENDADO);
        verify(appointmentRepository, never()).saveAllAndFlush(any());
    }

    private static Clinic clinic(UUID id) {
        Clinic clinic = Clinic.builder().id(id).name("Clínica").status(StatusClinic.ATIVO).build();
        return clinic;
    }

    private static ClinicUnit unit(UUID id, Clinic clinic) {
        ClinicUnit unit = new ClinicUnit();
        unit.setId(id);
        unit.setClinic(clinic);
        unit.setName("Matriz");
        unit.setTimeZone("America/Cuiaba");
        unit.setStatus(StatusUnit.ATIVO);
        return unit;
    }

    private static Appointment appointment(
            Clinic clinic, ClinicUnit unit, AppointmentStatus status, String startsAt) {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setName("Paciente");
        ServiceOffering service = new ServiceOffering();
        service.setId(UUID.randomUUID());
        service.setName("Consulta");
        service.setDurationMinutes(30);
        service.setCapacity(10);
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setClinic(clinic);
        appointment.setUnit(unit);
        appointment.setPatient(patient);
        appointment.setService(service);
        appointment.setStartsAt(Instant.parse(startsAt));
        appointment.setDurationMinutes(30);
        appointment.setStatus(status);
        appointment.setPrice(BigDecimal.ZERO);
        return appointment;
    }
}
