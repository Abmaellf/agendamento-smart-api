package com.agendamento.smart.dtos;

import com.agendamento.smart.model.user.UserRole;

public record RegisterDTO(String login, String password, UserRole role) {
}
