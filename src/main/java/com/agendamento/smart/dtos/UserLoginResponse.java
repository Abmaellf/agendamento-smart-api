package com.agendamento.smart.dtos;

import com.agendamento.smart.model.user.UserRole;

import java.util.UUID;

public record UserLoginResponse(
        UUID id,
        String login,
        UserRole role,
        UUID clinicId,
        UUID activeUnitId
) {
}
