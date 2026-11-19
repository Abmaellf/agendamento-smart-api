package com.agendamento.smart.dtos.appointment;

import com.agendamento.smart.model.appointment.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record AppointmentStatusRequest(@NotNull AppointmentStatus status) {
}
