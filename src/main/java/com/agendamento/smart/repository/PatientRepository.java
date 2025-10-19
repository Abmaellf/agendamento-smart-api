package com.agendamento.smart.repository;

import com.agendamento.smart.model.patient.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {

    Page<Patient> findAll(Pageable pageable);

        /* @Query(value = "SELECT * FROM tb_book WHERE publisher_id = :id", nativeQuery = true)
        List<Patient> findPatientByClinicId(@Param("id") String id); */

}
