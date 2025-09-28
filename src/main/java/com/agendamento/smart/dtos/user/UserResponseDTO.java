package com.agendamento.smart.dtos.user;

import com.agendamento.smart.model.user.UserRole;

public record UserResponseDTO(String login, UserRole role, String clinicId,  String clinicName) {
}
