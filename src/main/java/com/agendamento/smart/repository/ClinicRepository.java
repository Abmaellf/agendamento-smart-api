package com.agendamento.smart.repository;

import com.agendamento.smart.model.clinic.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, String> {

}
