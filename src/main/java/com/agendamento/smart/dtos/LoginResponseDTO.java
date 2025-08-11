package com.agendamento.smart.dtos;

import com.agendamento.smart.model.user.User;

public record LoginResponseDTO(String token, User user) {
}
