package com.agendamento.smart.service;

import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinicRepository;

    @Transactional
    public Clinic createClinic(UUID id, String name) {
        Clinic clinic = Clinic.builder()
                .id(id)
                .name(name)
                .build();
        return clinicRepository.save(clinic); // o code é gerado automaticamente
    }
}
