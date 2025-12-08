package com.agendamento.smart.repository;

import com.agendamento.smart.model.clinic.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
//    @Query(value = "SELECT * FROM Clinic WHERE BIN_TO_UUID(id, 1) = :uuid", nativeQuery = true)
//    Optional<Clinic> findByUuid(@Param("uuid") String uuid);


    @Query(
            value = "SELECT * FROM CLINIC WHERE id = UUID_TO_BIN(:id, 1)",
            nativeQuery = true
    )
    Optional<Clinic> findByUuid(String id);
//    Optional<Clinic> findById(UUID id);


    @Query("SELECT MAX(c.code) FROM Clinic c")
    Optional<Long> findMaxCode();
}
