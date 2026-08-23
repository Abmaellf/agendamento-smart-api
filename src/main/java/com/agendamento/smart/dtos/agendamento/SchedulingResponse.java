package com.agendamento.smart.dtos.agendamento;

import com.agendamento.smart.model.scheduling.StatusScheduling;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SchedulingResponse(
        UUID id,
        UUID tenantId,
        UUID unitId,
        ReferenceResponse patient,
        ReferenceResponse service,
        ReferenceResponse professional,
        String startsAt,
        String timeZone,
        Integer durationMinutes,
        BigDecimal price,
        StatusScheduling status,
        UUID createdBy,
        Instant createdAt,
        UUID patientId,
        List<String> pathology,
        LocalDateTime dateScheduling,
        LocalTime hours,
        String variant,
        Instant updatedAt
) {
    public record ReferenceResponse(UUID id, String name) {
    }
}
