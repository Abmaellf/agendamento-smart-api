package com.agendamento.smart.dtos.servicepackage;
import jakarta.validation.constraints.NotNull;
public record ServicePackageStatusRequest(@NotNull Boolean active) {}
