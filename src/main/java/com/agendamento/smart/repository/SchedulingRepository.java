package com.agendamento.smart.repository;
import com.agendamento.smart.model.scheduling.Scheduling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SchedulingRepository extends JpaRepository<Scheduling, UUID> {

    Optional<Scheduling> findById(UUID id);

    Optional<Scheduling> findByIdAndClinicId(UUID id, UUID clinicId);

    Optional<Scheduling> findByClinicIdAndIdempotencyKey(UUID clinicId, String idempotencyKey);

    List<Scheduling> findAllByClinicIdAndStartsAtIsNotNull(UUID clinicId);

    List<Scheduling> findAllByClinicIdAndUnitIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            UUID clinicId, UUID unitId, Instant from, Instant to);
}
