package com.agendamento.smart.dtos.patient;


import com.agendamento.smart.model.clinic.Clinic;

public record PatientRequestDTO(String name, String clinicId) {
}
