package com.agendamento.smart.service;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.dtos.servicepackage.ServicePackageRequest;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.servicepackage.PackageWeekDay;
import com.agendamento.smart.model.servicepackage.ServicePackageDefinition;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.ServiceOfferingRepository;
import com.agendamento.smart.repository.ServicePackageRepository;
import com.agendamento.smart.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicePackageServiceTest {

    @Mock private ServicePackageRepository packageRepository;
    @Mock private UnitRepository unitRepository;
    @Mock private ServiceOfferingRepository serviceRepository;
    @Mock private ServicePackageUsageService usageService;
    @Mock private UnitScopeService unitScopeService;
    @InjectMocks private ServicePackageService service;

    @Test
    void rejectsRepeatedWeekDaysBeforePersistingPackage() {
        User admin = admin();
        ServicePackageRequest request = new ServicePackageRequest("Pacote", null, UUID.randomUUID(), UUID.randomUUID(),
                10, new BigDecimal("400.00"), List.of(PackageWeekDay.MONDAY, PackageWeekDay.MONDAY), true);

        assertThatThrownBy(() -> service.create(admin, request))
                .isInstanceOfSatisfying(DomainException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("PACKAGE_VALIDATION_ERROR"));
        verify(packageRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refusesStatusChangeWhenFuturePackageAppointmentRelationReportsUsage() {
        User admin = admin();
        UUID packageId = UUID.randomUUID();
        ServicePackageDefinition definition = new ServicePackageDefinition();
        definition.setId(packageId);
        when(packageRepository.findByIdAndClinicId(packageId, admin.getClinic().getId())).thenReturn(Optional.of(definition));
        when(usageService.hasPackageAppointments(packageId)).thenReturn(true);

        assertThatThrownBy(() -> service.updateStatus(admin, packageId, false))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("PACKAGE_IMMUTABLE");
                    assertThat(exception.getStatus().value()).isEqualTo(409);
                });
        verify(packageRepository, never()).saveAndFlush(definition);
    }

    @Test
    void basicListingRejectsAUnitOutsideItsSessionScope() {
        Clinic clinic = Clinic.builder().id(UUID.randomUUID()).name("Clínica").build();
        ClinicUnit ownUnit = unit(clinic, "Matriz");
        ClinicUnit foreignUnit = unit(clinic, "Filial");
        User basic = User.builder()
                .clinic(clinic)
                .clinicUnit(ownUnit)
                .role(UserRole.USER)
                .build();
        when(unitScopeService.requireSingleAllowedUnitId(basic)).thenReturn(ownUnit.getId());

        assertThatThrownBy(() -> service.findAll(
                basic, foreignUnit.getId(), null, true, null, null))
                .isInstanceOfSatisfying(DomainException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TENANT_RESOURCE_NOT_FOUND"));
        verify(packageRepository, never()).findAllByClinicIdOrderByNameAsc(clinic.getId());
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

    private User admin() {
        Clinic clinic = Clinic.builder().id(UUID.randomUUID()).name("Clínica").build();
        return User.builder().clinic(clinic).role(UserRole.ADMIN).build();
    }
}
