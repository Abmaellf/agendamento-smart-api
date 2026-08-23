package com.agendamento.smart.dtos.catalog;

import java.util.List;
import java.util.UUID;

public record ProfessionalOptionResponse(
        UUID id,
        String name,
        UUID tenantId,
        List<UUID> serviceIds,
        List<Integer> availableWeekDays
) {
}
