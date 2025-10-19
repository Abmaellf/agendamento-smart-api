package com.agendamento.smart.mapper;

import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.model.patient.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PatientsMapper {
    PatientsMapper INSTANCE = Mappers.getMapper(PatientsMapper.class);
    Patient toEntity(PatientRequestDTO dto);

    PatientResponseDTO toDto(Patient entity);
}
