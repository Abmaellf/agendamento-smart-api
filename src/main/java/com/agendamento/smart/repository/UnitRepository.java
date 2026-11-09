package com.agendamento.smart.repository;

import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.unit.TypeUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository extends JpaRepository<ClinicUnit, UUID> {
    List<ClinicUnit> findAllByClinicId(UUID clinicId);
    Optional<ClinicUnit> findByIdAndClinicId(UUID id, UUID clinicId);
    Optional<ClinicUnit> findFirstByClinicIdOrderByNameAsc(UUID clinicId);
    Optional<ClinicUnit> findByClinicIdAndType(UUID clinicId, TypeUnit type);
    long countByClinicId(UUID clinicId);
    List<ClinicUnit> findAllByClinicIdAndIdIn(UUID clinicId, Collection<UUID> ids);
}
