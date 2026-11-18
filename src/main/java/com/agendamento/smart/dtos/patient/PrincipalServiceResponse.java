package com.agendamento.smart.dtos.patient;

import java.util.UUID;

public record PrincipalServiceResponse(
        UUID id,
        String name
) {
}