package com.agendamento.smart.dtos.catalog;

import java.util.UUID;

public record ServiceOptionResponse(
        UUID id,
        String name,
        UUID tenantId,
        Integer durationMinutes,
        String price,
        Integer capacity
) {
}
