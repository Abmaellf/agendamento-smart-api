package com.agendamento.smart.dtos.packageappointment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record PackageAppointmentSession(int sequence, LocalDate date, LocalTime time, Instant startsAt) {}
