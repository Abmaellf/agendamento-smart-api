package com.agendamento.smart.dtos.patient;

import java.util.List;
import java.util.UUID;

public record PatientResponseDTO(UUID id, String name, UUID clinicId, List<UUID> unitIds) {
}
