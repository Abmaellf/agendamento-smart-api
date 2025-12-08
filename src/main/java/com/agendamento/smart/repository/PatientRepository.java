package com.agendamento.smart.repository;

import com.agendamento.smart.model.patient.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Page<Patient> findAll(Pageable pageable);

    @Query("SELECT MAX(p.code) FROM Patient p")
    Optional<Long> findMaxCode();

    Optional<Patient> findByName(String name);

        /* @Query(value = "SELECT * FROM tb_book WHERE publisher_id = :id", nativeQuery = true)
        List<Patient> findPatientByClinicId(@Param("id") String id); */

}
