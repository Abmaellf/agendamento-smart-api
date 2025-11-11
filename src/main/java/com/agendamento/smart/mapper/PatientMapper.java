package com.agendamento.smart.mapper;

import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.model.patient.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "clinic", ignore = true) // será setado no service
    Patient toEntity(PatientRequestDTO dto);

    @Mapping(target = "clinicId", source = "clinic.id")
    PatientResponseDTO toDto(Patient entity);
}
