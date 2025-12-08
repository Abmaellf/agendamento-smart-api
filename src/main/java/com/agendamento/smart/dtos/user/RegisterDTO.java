package com.agendamento.smart.dtos.user;

import com.agendamento.smart.model.user.UserRole;

import java.util.UUID;

public record RegisterDTO(String login, String password, UserRole role) {
}
