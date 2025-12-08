package com.agendamento.smart.dtos.user;

import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.user.UserRole;

public record UserResponseDTO(String login, UserRole role, Clinic clinic) {
}
