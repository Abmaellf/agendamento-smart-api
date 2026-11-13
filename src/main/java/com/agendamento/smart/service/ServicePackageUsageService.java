package com.agendamento.smart.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ServicePackageUsageService {
    /** A SPEC 02 substituirá esta fronteira por consulta ao vínculo de agendamento de pacote. */
    public boolean hasPackageAppointments(UUID servicePackageId) {
        return false;
    }
}
