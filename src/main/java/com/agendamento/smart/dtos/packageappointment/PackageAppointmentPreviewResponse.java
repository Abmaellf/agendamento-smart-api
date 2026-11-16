package com.agendamento.smart.dtos.packageappointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PackageAppointmentPreviewResponse(UUID packageId, UUID patientId, UUID unitId, BigDecimal totalPrice, LocalDate firstDate, LocalDate lastDate, List<PackageAppointmentSession> sessions, String fingerprint) {}
