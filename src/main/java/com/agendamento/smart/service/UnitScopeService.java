package com.agendamento.smart.service;

import com.agendamento.smart.controller.exception.DomainException;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.UserRole;
import com.agendamento.smart.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnitScopeService {

    private final UnitRepository unitRepository;

    @Transactional(readOnly = true)
    public List<ClinicUnit> allowedUnits(User user) {
        UUID clinicId = user.getClinic().getId();
        if (user.getRole() == UserRole.ADMIN) {
            return unitRepository.findAllByClinicId(clinicId);
        }

        if (user.getClinicUnit() == null) {
            throw DomainException.tenantResourceNotFound();
        }

        ClinicUnit unit = unitRepository
                .findByIdAndClinicId(user.getClinicUnit().getId(), clinicId)
                .orElseThrow(DomainException::tenantResourceNotFound);
        return List.of(unit);
    }

    public boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }

    public UUID requireSingleAllowedUnitId(User user) {
        List<ClinicUnit> units = allowedUnits(user);
        if (units.size() != 1) {
            throw DomainException.tenantResourceNotFound();
        }
        return units.getFirst().getId();
    }
}
