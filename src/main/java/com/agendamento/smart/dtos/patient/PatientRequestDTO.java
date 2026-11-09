package com.agendamento.smart.dtos.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PatientRequestDTO(
        @NotBlank @Size(max = 255) String name,
        UUID clinicId,
        @NotEmpty List<@NotNull UUID> unitIds
) {
}
