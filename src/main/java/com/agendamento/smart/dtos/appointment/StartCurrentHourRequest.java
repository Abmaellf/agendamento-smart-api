package com.agendamento.smart.dtos.appointment;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record StartCurrentHourRequest(
        @NotNull UUID unitId,
        @NotNull Instant hourStart
) {
}
