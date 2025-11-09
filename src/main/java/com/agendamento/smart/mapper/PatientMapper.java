package com.agendamento.smart.mapper;

import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.model.patient.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PatientMapper {
 PatientMapper INSTANCE = Mappers.getMapper(PatientMapper.class);
    // Do DTO → Entidade
    @Mapping(target = "clinic.id",   ignore = true) // você define manualmente depois
    Patient toEntity(PatientRequestDTO dto);

    // Da Entidade → DTO de resposta
    @Mapping(target = "clinicId", ignore = true)
    PatientResponseDTO toDto(Patient entity);
}
