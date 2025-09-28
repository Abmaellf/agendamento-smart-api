package com.agendamento.smart.mapper;

import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.patient.Patient;

public class PatientMapper {
    public static Patient toEntity(PatientRequestDTO dto, Clinic clinic) {
        Patient patient = new Patient();
        patient.setName(dto.name());
        patient.setClinic(clinic);
        return patient;
    }

    public static PatientResponseDTO toDTO(Patient patient){
        return  new PatientResponseDTO(
                patient.getName(),
                patient.getClinic() != null ? patient.getClinic().getId() : null,
                patient.getClinic() != null ? patient.getClinic().getName() : null
        );
    }
}
