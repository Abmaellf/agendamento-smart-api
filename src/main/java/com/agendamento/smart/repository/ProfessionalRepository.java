package com.agendamento.smart.repository;

import com.agendamento.smart.model.professional.Professional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {
    @EntityGraph(attributePaths = "services")
    List<Professional> findDistinctByClinicIdAndServicesId(UUID clinicId, UUID serviceId);

    @EntityGraph(attributePaths = "services")
    Optional<Professional> findByIdAndClinicId(UUID id, UUID clinicId);
}
