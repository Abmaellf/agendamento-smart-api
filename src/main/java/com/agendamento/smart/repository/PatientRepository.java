package com.agendamento.smart.repository;

import com.agendamento.smart.model.patient.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {

//    @Query(value = "SELECT * FROM tb_book WHERE publisher_id = :id", nativeQuery = true)
//    List<Patient> findPatientByClinicId(@Param("id") String id);
}
