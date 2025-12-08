package com.agendamento.smart.repository;
import com.agendamento.smart.model.scheduling.Scheduling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SchedulingRepository extends JpaRepository<Scheduling, UUID> {

    Optional<Scheduling> findById(UUID id);
}
