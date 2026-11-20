package com.agendamento.smart.dtos.appointment;

import com.agendamento.smart.model.appointment.AppointmentStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record StartCurrentHourResponse(
        UUID unitId,
        String timeZone,
        Instant hourStart,
        Instant hourEnd,
        Instant evaluatedAt,
        AppointmentStatus targetStatus,
        int matchedCount,
        int updatedCount,
        int skippedCount,
        Map<AppointmentStatus, Integer> skippedByStatus
) {
}
