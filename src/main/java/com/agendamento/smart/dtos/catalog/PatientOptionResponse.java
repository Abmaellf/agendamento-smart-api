package com.agendamento.smart.dtos.catalog;

import java.util.UUID;

public record PatientOptionResponse(UUID id, String name, UUID tenantId) {
}
