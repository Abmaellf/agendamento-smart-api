package com.agendamento.smart.dtos.servicepackage;
import com.agendamento.smart.model.servicepackage.PackageWeekDay;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public record ServicePackageRequest(@NotBlank String name, String description, @NotNull UUID unitId, @NotNull UUID serviceId, @NotNull @Positive Integer totalSessions, @NotNull @DecimalMin(value = "0.00") BigDecimal totalPrice, @NotEmpty List<PackageWeekDay> weekDays, @NotNull Boolean active) {}
