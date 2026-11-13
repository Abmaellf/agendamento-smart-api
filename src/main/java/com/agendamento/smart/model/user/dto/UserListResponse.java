package com.agendamento.smart.model.user.dto;

import com.agendamento.smart.model.user.UserRole;

import java.util.UUID;

public record UserListResponse(
        UUID id,
        String login,
        UserRole role,
        UUID clinicId,
        UUID unitId,
        String unitName
) {
}
