package com.agendamento.smart.mapper;

import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.unit.ClinicUnit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "clinic", ignore = true) // será setado no service
    @Mapping(target = "units", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Patient toEntity(PatientRequestDTO dto);

    default PatientResponseDTO toDto(Patient entity) {
        return new PatientResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getClinic().getId(),
                entity.getUnits().stream()
                        .map(ClinicUnit::getId)
                        .sorted()
                        .toList());
    }
}
