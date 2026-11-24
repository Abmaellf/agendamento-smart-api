package com.agendamento.smart.service.packageappointment;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.dtos.packageappointment.PackageAppointmentPreviewResponse;
import com.agendamento.smart.dtos.packageappointment.PackageAppointmentRequest;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.packageappointment.PackageAppointmentContract;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.servicepackage.PackageWeekDay;
import com.agendamento.smart.model.servicepackage.ServicePackageDefinition;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.PackageAppointmentContractRepository;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.ServicePackageRepository;
import com.agendamento.smart.service.UnitScopeService;
import com.agendamento.smart.service.appointment.AppointmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class PackageAppointmentServiceTest {

    @Mock private ServicePackageRepository packageRepository;
    @Mock private PackageAppointmentContractRepository contractRepository;
    @Mock private AppointmentService appointmentService;
    @Mock private PatientRepository patientRepository;
    @Mock private UnitScopeService unitScopeService;
    @InjectMocks private PackageAppointmentService service;

    @Test
    void previewPreservesTheAuthorizedUnitInItsResponseAndFingerprint() {
        Fixture fixture = fixture(UserRole.ADMIN, fixtureUnitId());
        when(unitScopeService.allowedUnits(fixture.user())).thenReturn(List.of(fixture.unit()));
        when(packageRepository.findByIdAndClinicId(fixture.definition().getId(), fixture.clinic().getId()))
                .thenReturn(Optional.of(fixture.definition()));
        when(patientRepository.findByIdAndClinicIdWithUnits(fixture.patient().getId(), fixture.clinic().getId()))
                .thenReturn(Optional.of(fixture.patient()));

        PackageAppointmentPreviewResponse response = service.preview(fixture.request(), fixture.user());

        assertThat(response.unitId()).isEqualTo(fixture.unit().getId());
        assertThat(response.fingerprint()).isNotBlank();

        ClinicUnit secondUnit = unit(fixture.clinic(), "Filial");
        fixture.patient().setUnits(new LinkedHashSet<>(Set.of(fixture.unit(), secondUnit)));
        fixture.definition().setUnit(secondUnit);
        when(unitScopeService.allowedUnits(fixture.user())).thenReturn(List.of(fixture.unit(), secondUnit));
        PackageAppointmentRequest secondRequest = new PackageAppointmentRequest(
                fixture.request().packageId(), fixture.request().patientId(), secondUnit.getId(),
                fixture.request().startDate(), fixture.request().time(), fixture.request().professionalId(),
                fixture.request().effectiveWeekDays());

        PackageAppointmentPreviewResponse secondResponse = service.preview(secondRequest, fixture.user());

        assertThat(secondResponse.fingerprint()).isNotEqualTo(response.fingerprint());
    }

    @Test
    void previewRejectsPatientFromAnotherUnit() {
        Fixture fixture = fixture(UserRole.ADMIN, fixtureUnitId());
        ClinicUnit otherUnit = unit(fixture.clinic(), "Outra unidade");
        fixture.patient().setUnits(new LinkedHashSet<>(Set.of(otherUnit)));
        when(unitScopeService.allowedUnits(fixture.user())).thenReturn(List.of(fixture.unit()));
        when(packageRepository.findByIdAndClinicId(fixture.definition().getId(), fixture.clinic().getId()))
                .thenReturn(Optional.of(fixture.definition()));
        when(patientRepository.findByIdAndClinicIdWithUnits(fixture.patient().getId(), fixture.clinic().getId()))
                .thenReturn(Optional.of(fixture.patient()));

        assertThatThrownBy(() -> service.preview(fixture.request(), fixture.user()))
                .isInstanceOfSatisfying(DomainException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TENANT_RESOURCE_NOT_FOUND"));
    }

    @Test
    void basicCannotPreviewAnotherUnit() {
        UUID ownUnitId = fixtureUnitId();
        Fixture fixture = fixture(UserRole.USER, ownUnitId)
                .withRequestUnit(fixtureUnitId());
        when(unitScopeService.allowedUnits(fixture.user())).thenReturn(List.of(fixture.unit()));

        assertThatThrownBy(() -> service.preview(fixture.request(), fixture.user()))
                .isInstanceOfSatisfying(DomainException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TENANT_RESOURCE_NOT_FOUND"));
        verify(packageRepository, never()).findByIdAndClinicId(
                fixture.definition().getId(), fixture.clinic().getId());
    }

    @Test
    void confirmRejectsAnIncompatiblePatientBeforePersistence() {
        Fixture fixture = fixture(UserRole.ADMIN, fixtureUnitId());
        ClinicUnit otherUnit = unit(fixture.clinic(), "Outra unidade");
        fixture.patient().setUnits(new LinkedHashSet<>(Set.of(otherUnit)));
        when(contractRepository.findByClinicIdAndIdempotencyKey(fixture.clinic().getId(), "key"))
                .thenReturn(Optional.empty());
        when(unitScopeService.allowedUnits(fixture.user())).thenReturn(List.of(fixture.unit()));
        when(packageRepository.findByIdAndClinicId(fixture.definition().getId(), fixture.clinic().getId()))
                .thenReturn(Optional.of(fixture.definition()));
        when(patientRepository.findByIdAndClinicIdWithUnits(fixture.patient().getId(), fixture.clinic().getId()))
                .thenReturn(Optional.of(fixture.patient()));

        assertThatThrownBy(() -> service.confirm(fixture.request(), fixture.user(), "key", null))
                .isInstanceOf(DomainException.class);

        verify(contractRepository, never()).saveAndFlush(any(PackageAppointmentContract.class));
        verify(appointmentService, never()).create(any(), any(), any());
    }

    private Fixture fixture(UserRole role, UUID unitId) {
        Clinic clinic = Clinic.builder().id(UUID.randomUUID()).name("Clínica").build();
        ClinicUnit unit = unit(clinic, "Matriz");
        unit.setId(unitId);
        User user = User.builder()
                .id(UUID.randomUUID())
                .clinic(clinic)
                .clinicUnit(unit)
                .role(role)
                .build();
        ServiceOffering serviceOffering = new ServiceOffering();
        serviceOffering.setId(UUID.randomUUID());
        serviceOffering.setClinic(clinic);
        serviceOffering.setName("Pilates");
        serviceOffering.setDurationMinutes(60);
        serviceOffering.setPrice(new BigDecimal("125.00"));
        serviceOffering.setCapacity(2);
        ServicePackageDefinition definition = new ServicePackageDefinition();
        definition.setId(UUID.randomUUID());
        definition.setClinic(clinic);
        definition.setUnit(unit);
        definition.setService(serviceOffering);
        definition.setCreatedBy(user);
        definition.setName("Pacote");
        definition.setTotalSessions(1);
        definition.setTotalPrice(new BigDecimal("125.00"));
        definition.setActive(true);
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setClinic(clinic);
        patient.setUnits(new LinkedHashSet<>(Set.of(unit)));
        PackageAppointmentRequest request = new PackageAppointmentRequest(
                definition.getId(),
                patient.getId(),
                unit.getId(),
                LocalDate.of(2026, 8, 24),
                LocalTime.of(8, 30),
                null,
                Set.of(PackageWeekDay.MONDAY));
        return new Fixture(clinic, unit, user, definition, patient, request);
    }

    private ClinicUnit unit(Clinic clinic, String name) {
        ClinicUnit unit = new ClinicUnit();
        unit.setId(UUID.randomUUID());
        unit.setClinic(clinic);
        unit.setTenantId(clinic.getId());
        unit.setName(name);
        unit.setTimeZone("America/Cuiaba");
        return unit;
    }

    private UUID fixtureUnitId() {
        return UUID.randomUUID();
    }

    private record Fixture(
            Clinic clinic,
            ClinicUnit unit,
            User user,
            ServicePackageDefinition definition,
            Patient patient,
            PackageAppointmentRequest request) {
        private Fixture withRequestUnit(UUID requestUnitId) {
            return new Fixture(clinic, unit, user, definition, patient,
                    new PackageAppointmentRequest(
                            request.packageId(), request.patientId(), requestUnitId,
                            request.startDate(), request.time(), request.professionalId(),
                            request.effectiveWeekDays()));
        }
    }
}
