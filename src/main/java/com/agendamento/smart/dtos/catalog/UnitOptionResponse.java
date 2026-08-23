package com.agendamento.smart.dtos.catalog;

import java.util.UUID;

public record UnitOptionResponse(UUID id, String name, UUID tenantId, String timeZone) {
}
