package com.agendamento.smart.dtos.clinic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClinicRegistrationRequest(
        @NotNull @Valid ClinicData clinic,
        @NotNull @Valid MatrixUnitData matrix,
        @NotNull @Valid InitialProfessionalData attendant,
        @NotNull @Valid InitialProfessionalData administrative
) {
    public record ClinicData(
            @NotBlank @Size(max = 255) String name
    ) {
    }

    public record MatrixUnitData(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 80) String timeZone
    ) {
    }

    public record InitialProfessionalData(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 255) String login,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }
}
