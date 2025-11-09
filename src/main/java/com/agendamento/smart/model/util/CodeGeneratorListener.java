package com.agendamento.smart.model.util;

import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.PatientRepository;
import jakarta.persistence.PrePersist;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CodeGeneratorListener {

    private static ClinicRepository clinicRepository;
    private static PatientRepository patientRepository;

    @Autowired
    public void init(ClinicRepository clinicRepo, PatientRepository patientRepo) {
        clinicRepository = clinicRepo;
        patientRepository = patientRepo;
    }

    @PrePersist
    @Transactional
    public void generateCode(Object entity) {
        if (entity instanceof Clinic clinic) {
            if (clinic.getCode() == null) {
                Long next = clinicRepository.findMaxCode().map(c -> c + 1).orElse(1000L);
                clinic.setCode(next);
            }
        } else if (entity instanceof Patient patient) {
            if (patient.getCode() == null) {
                Long next = patientRepository.findMaxCode().map(c -> c + 1).orElse(5000L);
                patient.setCode(next);
            }
        }
    }
}