package com.agendamento.smart.dtos.clinic;

import com.agendamento.smart.model.clinic.StatusClinic;
import com.agendamento.smart.model.professional.TypeProfessional;
import com.agendamento.smart.model.unit.StatusUnit;
import com.agendamento.smart.model.unit.TypeUnit;
import com.agendamento.smart.model.user.UserRole;

import java.util.UUID;

public record ClinicRegistrationResponse(
        UUID clinicId,
        StatusClinic clinicStatus,
        UUID unitId,
        StatusUnit unitStatus,
        TypeUnit unitType,
        InitialAccountResponse attendant,
        InitialAccountResponse administrative
) {
    public record InitialAccountResponse(
            UUID userId,
            UUID professionalId,
            UserRole role,
            TypeProfessional professionalType
    ) {
    }
}
