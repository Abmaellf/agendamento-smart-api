package com.agendamento.smart.repository;

import com.agendamento.smart.model.clinic.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
    @Query("SELECT MAX(c.code) FROM Clinic c")
    Optional<Long> findMaxCode();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Clinic c where c.id = :id")
    Optional<Clinic> findByIdForUpdate(@Param("id") UUID id);
}
