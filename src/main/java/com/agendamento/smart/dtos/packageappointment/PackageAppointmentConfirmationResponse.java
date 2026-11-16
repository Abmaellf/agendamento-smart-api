package com.agendamento.smart.dtos.packageappointment;

import java.util.List;
import java.util.UUID;

public record PackageAppointmentConfirmationResponse(UUID contractId, List<UUID> appointmentIds, PackageAppointmentPreviewResponse preview) {}
