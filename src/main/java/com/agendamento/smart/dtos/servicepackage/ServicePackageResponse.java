package com.agendamento.smart.dtos.servicepackage;
import com.agendamento.smart.model.servicepackage.PackageWeekDay;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record ServicePackageResponse(UUID id, UUID unitId, String unitName, UUID serviceId, String serviceName, String name, String description, Integer totalSessions, String totalPrice, List<PackageWeekDay> weekDays, Integer weeklyFrequency, boolean active, UUID createdBy, Instant createdAt, Instant updatedAt) {}
