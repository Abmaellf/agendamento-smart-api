package com.agendamento.smart.dtos.patient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PatientResponseDTO(
             UUID id,
             Long code,
             String name,
             UUID clinicId,
             List<UUID> unitIds,
             LocalDateTime createdAt,
             String status,
             PrincipalServiceResponse principalService)
{}
