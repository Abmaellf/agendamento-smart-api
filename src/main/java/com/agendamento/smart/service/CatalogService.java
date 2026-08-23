package com.agendamento.smart.service;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.dtos.catalog.PatientOptionResponse;
import com.agendamento.smart.dtos.catalog.ProfessionalOptionResponse;
import com.agendamento.smart.dtos.catalog.ServiceOptionResponse;
import com.agendamento.smart.dtos.catalog.UnitOptionResponse;
import com.agendamento.smart.dtos.catalog.UpdateServiceRequest;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.ProfessionalRepository;
import com.agendamento.smart.repository.ServiceOfferingRepository;
import com.agendamento.smart.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final PatientRepository patientRepository;
    private final UnitRepository unitRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;

    @Transactional(readOnly = true)
    public List<PatientOptionResponse> patients(User user) {
        UUID clinicId = user.getClinic().getId();
        UUID tenantId = tenantId(user);
        return patientRepository.findAllByClinicId(clinicId).stream()
                .map(patient -> new PatientOptionResponse(patient.getId(), patient.getName(), tenantId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UnitOptionResponse> units(User user) {
        return unitRepository.findAllByClinicId(user.getClinic().getId()).stream()
                .map(unit -> new UnitOptionResponse(
                        unit.getId(), unit.getName(), unit.getTenantId(), unit.getTimeZone()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceOptionResponse> services(User user) {
        UUID tenantId = tenantId(user);
        return serviceRepository.findAllByClinicId(user.getClinic().getId()).stream()
                .map(service -> toResponse(service, tenantId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProfessionalOptionResponse> professionals(User user, UUID serviceId) {
        UUID clinicId = user.getClinic().getId();
        ServiceOffering service = serviceRepository.findByIdAndClinicId(serviceId, clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        UUID tenantId = tenantId(user);
        return professionalRepository.findDistinctByClinicIdAndServicesId(clinicId, service.getId()).stream()
                .map(professional -> new ProfessionalOptionResponse(
                        professional.getId(),
                        professional.getName(),
                        tenantId,
                        professional.getServices().stream().map(ServiceOffering::getId).toList(),
                        professional.availableWeekDays()))
                .toList();
    }

    @Transactional
    public ServiceOptionResponse updateService(User user, UUID id, UpdateServiceRequest request) {
        ServiceOffering service = serviceRepository.findByIdAndClinicId(id, user.getClinic().getId())
                .orElseThrow(DomainException::tenantResourceNotFound);
        if (request.durationMinutes() != null) service.setDurationMinutes(request.durationMinutes());
        if (request.price() != null) service.setPrice(new BigDecimal(request.price()));
        return toResponse(serviceRepository.save(service), tenantId(user));
    }

    private UUID tenantId(User user) {
        return unitRepository.findFirstByClinicIdOrderByNameAsc(user.getClinic().getId())
                .map(unit -> unit.getTenantId())
                .orElseThrow(() -> new DomainException(
                        "TENANT_RESOURCE_NOT_FOUND", "Cadastro não encontrado.", HttpStatus.NOT_FOUND));
    }

    private ServiceOptionResponse toResponse(ServiceOffering service, UUID tenantId) {
        return new ServiceOptionResponse(
                service.getId(),
                service.getName(),
                tenantId,
                service.getDurationMinutes(),
                service.getPrice().setScale(2).toPlainString(),
                service.getCapacity());
    }
}
