package com.agendamento.smart.service;

import com.agendamento.smart.dtos.PageResponseDTO;
import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.mapper.PatientMapper;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
        Patient patient = patientMapper.toEntity(dto);

        // 2️⃣ Buscar a clínica associada
        Clinic clinic = clinicRepository.findById(dto.clinicId())
                .orElseThrow(() -> new EntityNotFoundException("Clinic not found with ID: " + dto.clinicId()));
        System.out.println("Clinica encontrada "+clinic.getName());
        System.out.println("Paciente name"+patient.getName());
        System.out.println("Paciente id"+patient.getId());


        // 3️⃣ Setar o relacionamento
        patient.setClinic(clinic);

        System.out.println("Clinica name "+patient.getClinic().getName());
        System.out.println("Clinica code "+patient.getClinic().getCode());
        patientRepository.save(patient);

        return patientMapper.toDto(patient);
    }

}
