package com.agendamento.smart.repository;

import com.agendamento.smart.model.servicepackage.ServicePackageDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicePackageRepository extends JpaRepository<ServicePackageDefinition, UUID> {
    List<ServicePackageDefinition> findAllByClinicIdOrderByNameAsc(UUID clinicId);
    Optional<ServicePackageDefinition> findByIdAndClinicId(UUID id, UUID clinicId);
}
