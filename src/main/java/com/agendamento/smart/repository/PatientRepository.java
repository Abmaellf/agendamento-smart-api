package com.agendamento.smart.repository;

import com.agendamento.smart.model.patient.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Page<Patient> findAll(Pageable pageable);

    @Query("SELECT MAX(p.code) FROM Patient p")
    Optional<Long> findMaxCode();

    Optional<Patient> findByName(String name);

    List<Patient> findAllByClinicId(UUID clinicId);

    Optional<Patient> findByIdAndClinicId(UUID id, UUID clinicId);

    @Query("select distinct p from Patient p left join fetch p.units "
            + "where p.id = :id and p.clinic.id = :clinicId")
    Optional<Patient> findByIdAndClinicIdWithUnits(
            @Param("id") UUID id,
            @Param("clinicId") UUID clinicId);

    @Query("select distinct p from Patient p left join fetch p.units where p.id = :id")
    Optional<Patient> findByIdWithUnits(@Param("id") UUID id);

        /* @Query(value = "SELECT * FROM tb_book WHERE publisher_id = :id", nativeQuery = true)
        List<Patient> findPatientByClinicId(@Param("id") String id); */

}
