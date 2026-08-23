package com.agendamento.smart.dtos.agendamento;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SchedulingRequest(
        @NotNull UUID patientId,
        @NotNull UUID unitId,
        @NotNull UUID serviceId,
        UUID professionalId,
        @NotNull Instant startsAt,
        @NotNull @Min(1) @Max(480) Integer durationMinutes,
        @NotNull
        @PositiveOrZero
        @Digits(integer = 8, fraction = 2)
        BigDecimal price
) {
}
