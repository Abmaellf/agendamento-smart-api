package com.agendamento.smart.service;

import com.agendamento.smart.dtos.PageResponseDTO;
import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.mapper.PatientsMapper;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.repository.PatientRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PatientService implements PatientServiceInt{

    private final PatientRepository patientRepository;
    private final PatientsMapper patientMapper;
    @Override
    public PageResponseDTO<PatientResponseDTO> findAllPatient(Pageable pageable){

        var pageEntity = patientRepository.findAll(pageable);
        var pageDto = pageEntity.map(patientMapper::toDto);

        return PageResponseDTO.from(pageDto);
    }

    @Transactional
    public PatientResponseDTO save(PatientRequestDTO dto) {
        Patient patient = patientMapper.toEntity(dto);
        Patient saved = patientRepository.save(patient);

        return patientMapper.toDto(saved);
    }
}
