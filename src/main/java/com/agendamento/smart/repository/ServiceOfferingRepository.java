package com.agendamento.smart.repository;

import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {
    List<ServiceOffering> findAllByClinicId(UUID clinicId);
    Optional<ServiceOffering> findByIdAndClinicId(UUID id, UUID clinicId);
}
