package com.agendamento.smart.dtos.user;

import com.agendamento.smart.model.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterDTO(
        @NotBlank @Size(max = 255) String login,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull UserRole role,
        @NotNull Boolean isProfessional,
        @Valid ProfessionalData professional
) {
    @AssertTrue(message = "professional é obrigatório quando isProfessional=true")
    public boolean isProfessionalDataConsistent() {
        return !Boolean.TRUE.equals(isProfessional) || professional != null;
    }

    public record ProfessionalData(
            @NotBlank @Size(max = 255) String name,
            @NotNull @Size(min = 1) Set<@Min(1) @Max(7) Integer> availableWeekDays
    ) {
    }
}
