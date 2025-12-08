package com.agendamento.smart.dtos.agendamento;
import com.agendamento.smart.model.scheduling.StatusScheduling;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SchedulingRequest(
        @NotNull UUID patientId,
        @NotNull List<String> pathology,
        @NotNull LocalDateTime dateScheduling,
        @NotNull LocalTime hours,
        StatusScheduling status,
        String variant
) {}
