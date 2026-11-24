package com.agendamento.smart.dtos.packageappointment;

import com.agendamento.smart.model.servicepackage.PackageWeekDay;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record PackageAppointmentRequest(
        @NotNull UUID packageId,
        @NotNull UUID patientId,
        @NotNull UUID unitId,
        @NotNull LocalDate startDate,
        @NotNull LocalTime time,
        UUID professionalId,
        @NotEmpty Set<PackageWeekDay> effectiveWeekDays) {}
