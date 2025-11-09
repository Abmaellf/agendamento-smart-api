package com.agendamento.smart.dtos.patient;

import java.util.UUID;

public record PatientRequestDTO(String name, UUID clinicId) {
}
