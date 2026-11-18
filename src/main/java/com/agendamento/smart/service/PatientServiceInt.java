package com.agendamento.smart.service;

import com.agendamento.smart.dtos.PageResponseDTO;
import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface PatientServiceInt {

    PageResponseDTO<PatientResponseDTO> findAllPatient(
            Pageable pageable,
            String name,
            Authentication authentication);

    PatientResponseDTO save(
            PatientRequestDTO dto,
            Authentication authentication);
}
