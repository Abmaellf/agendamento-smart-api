package com.agendamento.smart.dtos.user;

import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.user.UserRole;

import java.util.UUID;

public record UserResponseDTO(String login, UserRole role, Clinic clinic) {
}
