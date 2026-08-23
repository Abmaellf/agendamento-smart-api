package com.agendamento.smart.model.user.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String login,
        String username,
        String name,
        String role,
        UUID clinicId,      // Enviando apenas o ID em vez do objeto todo
        UUID tenantId,
        UUID activeUnitId
) {}