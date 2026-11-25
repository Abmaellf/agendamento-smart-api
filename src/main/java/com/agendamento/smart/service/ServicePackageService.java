package com.agendamento.smart.service;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.dtos.PageResponseDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicePackageService {
    private final ServicePackageRepository packageRepository;
    private final UnitRepository unitRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final ServicePackageUsageService usageService;
    private final UnitScopeService unitScopeService;

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
    public PageResponseDTO<ServicePackageResponse> findAll(User user, UUID unitId, UUID serviceId, Boolean active,
            Integer totalSessions, String name, int page, int size) {
        validatePagination(page, size);
        if (user.getRole() != UserRole.ADMIN) {
            UUID allowedUnitId = unitScopeService.requireSingleAllowedUnitId(user);
            if (unitId != null && !unitId.equals(allowedUnitId)) {
                throw DomainException.tenantResourceNotFound();
            }
            unitId = allowedUnitId;
            active = true;
        } else if (unitId != null) {
            unitRepository.findByIdAndClinicId(unitId, user.getClinic().getId())
                    .orElseThrow(this::packageNotFound);
        }
        String normalizedName = name == null || name.isBlank() ? null : name.trim();
        Page<ServicePackageDefinition> packages = packageRepository.findPageByClinicIdAndFilters(
                user.getClinic().getId(), unitId, serviceId, active, totalSessions, normalizedName,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")));
        return PageResponseDTO.from(packages.map(this::toResponse));
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new DomainException(
                    "PACKAGE_PAGINATION_ERROR",
                    "Os parâmetros page e size são inválidos.",
                    HttpStatus.BAD_REQUEST);
        }
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
