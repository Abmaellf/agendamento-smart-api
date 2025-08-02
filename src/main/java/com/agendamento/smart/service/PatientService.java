package com.agendamento.smart.service;

import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {
    @Autowired
    private PatientRepository patientRepository;

    public  List<Patient> findAllPatient(){
        return patientRepository.findAll();
    }

    public Patient save(Patient patient) {
       return patientRepository.save(patient);
    }
}
