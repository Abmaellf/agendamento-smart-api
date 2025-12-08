package com.agendamento.smart.service.scheduling;
import com.agendamento.smart.dtos.agendamento.SchedulingRequest;
import com.agendamento.smart.dtos.agendamento.SchedulingResponse;
import com.agendamento.smart.mapper.SchedulingMapper;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.scheduling.Scheduling;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final SchedulingRepository schedulingRepository;
    private final PatientRepository patientRepository;
    private final SchedulingMapper mapper;


    public SchedulingResponse create(SchedulingRequest request) {

//        Patient patient = patientRepository.findById(request.patientId().toString())
//                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));

        Scheduling scheduling = mapper.toEntity(request);
        scheduling.setPatient(patient);

        return mapper.toResponse(schedulingRepository.save(scheduling));
    }

    public SchedulingResponse findById(UUID id) {
        Scheduling scheduling = schedulingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        return mapper.toResponse(scheduling);
    }
}
