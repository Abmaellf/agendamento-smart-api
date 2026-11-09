package com.agendamento.smart.repository;

import com.agendamento.smart.model.appointment.Appointment;
import com.agendamento.smart.model.unit.StatusUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select appointment
            from Appointment appointment
            where appointment.clinic.id = :clinicId
              and appointment.unit.status <> :excludedUnitStatus
              and appointment.startsAt >= :from
              and appointment.startsAt < :to
            order by appointment.startsAt asc
            """)
    List<Appointment> findReadableByClinicIdAndStartsAtBetween(
            @Param("clinicId") UUID clinicId,
            @Param("excludedUnitStatus") StatusUnit excludedUnitStatus,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
