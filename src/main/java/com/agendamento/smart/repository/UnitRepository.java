package com.agendamento.smart.repository;

import com.agendamento.smart.model.unit.ClinicUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository extends JpaRepository<ClinicUnit, UUID> {
    List<ClinicUnit> findAllByClinicId(UUID clinicId);
    Optional<ClinicUnit> findByIdAndClinicId(UUID id, UUID clinicId);
    Optional<ClinicUnit> findFirstByClinicIdOrderByNameAsc(UUID clinicId);
}
