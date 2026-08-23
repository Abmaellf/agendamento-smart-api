package com.agendamento.smart.dtos.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record UpdateServiceRequest(
        @Min(1) @Max(480) Integer durationMinutes,
        @Pattern(regexp = "^(?:0|[1-9]\\d*)(?:\\.\\d{2})$") String price
) {
}
