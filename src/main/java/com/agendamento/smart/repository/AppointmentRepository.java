package com.agendamento.smart.repository;

import com.agendamento.smart.model.appointment.Appointment;
import com.agendamento.smart.model.appointment.AppointmentStatus;
import com.agendamento.smart.model.unit.StatusUnit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findById(UUID id);

    Optional<Appointment> findByIdAndClinicId(UUID id, UUID clinicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select appointment
            from Appointment appointment
            where appointment.id = :id
              and appointment.clinic.id = :clinicId
            """)
    Optional<Appointment> findByIdAndClinicIdForUpdate(
            @Param("id") UUID id,
            @Param("clinicId") UUID clinicId);

    Optional<Appointment> findByClinicIdAndIdempotencyKey(UUID clinicId, String idempotencyKey);

    List<Appointment> findAllByClinicIdAndStartsAtIsNotNull(UUID clinicId);

    long countByStatusAndStartsAtLessThanEqual(AppointmentStatus status, Instant cutoff);

    long countByClinicIdAndStatusAndStartsAtLessThanEqual(
            UUID clinicId, AppointmentStatus status, Instant cutoff);

    @Modifying
    @Query("""
            update Appointment appointment
               set appointment.status = :targetStatus,
                   appointment.updatedAt = CURRENT_TIMESTAMP,
                   appointment.version = appointment.version + 1
             where appointment.status = :currentStatus
               and appointment.startsAt <= :cutoff
            """)
    int markOverdueAsNoShow(
            @Param("currentStatus") AppointmentStatus currentStatus,
            @Param("targetStatus") AppointmentStatus targetStatus,
            @Param("cutoff") Instant cutoff);

    @Modifying
    @Query("""
            update Appointment appointment
               set appointment.status = :targetStatus,
                   appointment.updatedAt = CURRENT_TIMESTAMP,
                   appointment.version = appointment.version + 1
             where appointment.clinic.id = :clinicId
               and appointment.status = :currentStatus
               and appointment.startsAt <= :cutoff
            """)
    int markClinicOverdueAsNoShow(
            @Param("clinicId") UUID clinicId,
            @Param("currentStatus") AppointmentStatus currentStatus,
            @Param("targetStatus") AppointmentStatus targetStatus,
            @Param("cutoff") Instant cutoff);

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
