package com.agendamento.smart.service;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.dtos.servicepackage.ServicePackageRequest;
import com.agendamento.smart.dtos.servicepackage.ServicePackageResponse;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.servicepackage.PackageWeekDay;
import com.agendamento.smart.model.servicepackage.ServicePackageDefinition;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.ServiceOfferingRepository;
import com.agendamento.smart.repository.ServicePackageRepository;
import com.agendamento.smart.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ServicePackageService {
    private final ServicePackageRepository packageRepository;
    private final UnitRepository unitRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final ServicePackageUsageService usageService;

    @Transactional
    public ServicePackageResponse create(User user, ServicePackageRequest request) {
        requireAdmin(user);
        ServicePackageDefinition definition = new ServicePackageDefinition();
        definition.setClinic(user.getClinic());
        definition.setCreatedBy(user);
        apply(definition, user, request);
        return toResponse(packageRepository.saveAndFlush(definition));
    }

    @Transactional(readOnly = true)
    public List<ServicePackageResponse> findAll(User user, UUID unitId, UUID serviceId, Boolean active, Integer totalSessions, String name) {
        requireAdmin(user);
        Stream<ServicePackageDefinition> packages = packageRepository
                .findAllByClinicIdOrderByNameAsc(user.getClinic().getId()).stream();
        if (unitId != null) packages = packages.filter(item -> item.getUnit().getId().equals(unitId));
        if (serviceId != null) packages = packages.filter(item -> item.getService().getId().equals(serviceId));
        if (active != null) packages = packages.filter(item -> item.isActive() == active);
        if (totalSessions != null) packages = packages.filter(item -> item.getTotalSessions().equals(totalSessions));
        if (name != null && !name.isBlank()) {
            String normalized = name.trim().toLowerCase();
            packages = packages.filter(item -> item.getName().toLowerCase().contains(normalized));
        }
        return packages.map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServicePackageResponse findById(User user, UUID id) {
        requireAdmin(user);
        return toResponse(findDefinition(user, id));
    }

    @Transactional
    public ServicePackageResponse update(User user, UUID id, ServicePackageRequest request) {
        requireAdmin(user);
        ServicePackageDefinition definition = findDefinition(user, id);
        requireMutable(definition);
        apply(definition, user, request);
        return toResponse(packageRepository.saveAndFlush(definition));
    }

    @Transactional
    public ServicePackageResponse updateStatus(User user, UUID id, boolean active) {
        requireAdmin(user);
        ServicePackageDefinition definition = findDefinition(user, id);
        requireMutable(definition);
        definition.setActive(active);
        return toResponse(packageRepository.saveAndFlush(definition));
    }

    private void apply(ServicePackageDefinition definition, User user, ServicePackageRequest request) {
        LinkedHashSet<PackageWeekDay> days = new LinkedHashSet<>(request.weekDays());
        if (days.size() != request.weekDays().size()) {
            throw new DomainException("PACKAGE_VALIDATION_ERROR", "Os dias da semana não podem se repetir.", HttpStatus.BAD_REQUEST);
        }
        UUID clinicId = user.getClinic().getId();
        ClinicUnit unit = unitRepository.findByIdAndClinicId(request.unitId(), clinicId)
                .orElseThrow(this::packageNotFound);
        ServiceOffering service = serviceRepository.findByIdAndClinicId(request.serviceId(), clinicId)
                .orElseThrow(this::packageNotFound);
        definition.setTenantId(unit.getTenantId());
        definition.setUnit(unit);
        definition.setService(service);
        definition.setName(request.name().trim());
        definition.setDescription(normalizeDescription(request.description()));
        definition.setTotalSessions(request.totalSessions());
        definition.setTotalPrice(request.totalPrice().setScale(2, RoundingMode.HALF_UP));
        definition.setWeekDays(days);
        definition.setActive(request.active());
    }

    private ServicePackageDefinition findDefinition(User user, UUID id) {
        return packageRepository.findByIdAndClinicId(id, user.getClinic().getId())
                .orElseThrow(this::packageNotFound);
    }

    private void requireMutable(ServicePackageDefinition definition) {
        if (usageService.hasPackageAppointments(definition.getId())) {
            throw new DomainException("PACKAGE_IMMUTABLE", "O pacote possui agendamentos vinculados e não pode ser alterado.", HttpStatus.CONFLICT);
        }
    }

    private void requireAdmin(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new DomainException("PACKAGE_FORBIDDEN", "Somente administradores podem gerenciar pacotes.", HttpStatus.FORBIDDEN);
        }
    }

    private DomainException packageNotFound() {
        return new DomainException("PACKAGE_NOT_FOUND", "Pacote, unidade ou serviço não encontrado no escopo autorizado.", HttpStatus.NOT_FOUND);
    }

    private String normalizeDescription(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ServicePackageResponse toResponse(ServicePackageDefinition definition) {
        List<PackageWeekDay> days = definition.getWeekDays().stream().sorted().toList();
        return new ServicePackageResponse(definition.getId(), definition.getUnit().getId(), definition.getUnit().getName(),
                definition.getService().getId(), definition.getService().getName(), definition.getName(), definition.getDescription(),
                definition.getTotalSessions(), definition.getTotalPrice().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                days, days.size(), definition.isActive(), definition.getCreatedBy().getId(), definition.getCreatedAt(), definition.getUpdatedAt());
    }
}
