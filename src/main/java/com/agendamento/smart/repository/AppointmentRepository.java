package com.agendamento.smart.repository;

import com.agendamento.smart.model.appointment.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findById(UUID id);

    Optional<Appointment> findByIdAndClinicId(UUID id, UUID clinicId);

    Optional<Appointment> findByClinicIdAndIdempotencyKey(UUID clinicId, String idempotencyKey);

    List<Appointment> findAllByClinicIdAndStartsAtIsNotNull(UUID clinicId);

    List<Appointment> findAllByClinicIdAndUnitIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            UUID clinicId, UUID unitId, Instant from, Instant to);
}
