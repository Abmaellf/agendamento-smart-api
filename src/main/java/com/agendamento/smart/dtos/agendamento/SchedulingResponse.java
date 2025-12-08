package com.agendamento.smart.dtos.agendamento;

import com.agendamento.smart.model.scheduling.StatusScheduling;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SchedulingResponse(
        UUID id,
        UUID patientId,
        List<String> pathology,
        LocalDateTime dateScheduling,
        LocalTime hours,
        StatusScheduling status,
        String variant,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
