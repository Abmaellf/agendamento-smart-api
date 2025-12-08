package com.agendamento.smart.service;

import com.agendamento.smart.dtos.PageResponseDTO;
import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.mapper.PatientMapper;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.PatientRepository;
import com.sun.jdi.request.DuplicateRequestException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class PatientService implements PatientServiceInt{

    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final PatientMapper patientMapper;
    @Override
    public PageResponseDTO<PatientResponseDTO> findAllPatient(Pageable pageable){

        var pageEntity = patientRepository.findAll(pageable);
        var pageDto = pageEntity.map(patientMapper::toDto);

        return PageResponseDTO.from(pageDto);
    }

    @Transactional
    public PatientResponseDTO save(PatientRequestDTO dto) {

//        Clinic clinic = clinicRepository.findByUuid(dto.clinicId().toString())
//                .orElseThrow(() -> new RuntimeException("Clinic not found"));

        Clinic clinic = clinicRepository.findById(dto.clinicId())
                .orElseThrow(() -> new RuntimeException("Clinic not found"));

        Patient patient = patientMapper.toEntity(dto);
        String namePatient = dto.name().toUpperCase();

        Optional<Patient> existsOpt = patientRepository.findByName(namePatient);

        existsOpt.ifPresent(p -> {
            throw new DuplicateRequestException("Patient already exists");
        });


        patient.setName(namePatient);
        patient.setClinic(clinic);

        patient = patientRepository.save(patient);
        System.out.println(patient.getName());

        return patientMapper.toDto(patient);
    }

}
