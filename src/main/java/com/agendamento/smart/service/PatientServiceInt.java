package com.agendamento.smart.service;

import com.agendamento.smart.dtos.PageResponseDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import org.springframework.data.domain.Pageable;

public interface PatientServiceInt {
    PageResponseDTO<PatientResponseDTO> findAllPatient(Pageable pageable);
}
