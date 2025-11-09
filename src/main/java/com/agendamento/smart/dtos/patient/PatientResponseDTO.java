package com.agendamento.smart.dtos.patient;

import java.util.UUID;

public record PatientResponseDTO(UUID id,String name, UUID clinicId) {
}
