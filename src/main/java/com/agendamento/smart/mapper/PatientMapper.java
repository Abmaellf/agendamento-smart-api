package com.agendamento.smart.mapper;

import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.model.patient.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    // Request → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinic", ignore = true) // você seta manualmente no service
    Patient toEntity(PatientRequestDTO dto);
    // Da Entidade para o DTO
    // id da Entidade  vai usar qual do dto (NÃO TEM POIS AO SALVAR SERÁ GERADO ENTÃO IGNORA)
    // o clinic da Entidade vai usar qual do dto (NÃO TEM POIS AO SALVAR SERÁ GERADO ENTÃO IGNORA)
    // o name da Entidade vai usar o name do DTO -> (será convertido automaticamente pois são iguais)

    // Entity → Response DTO
    @Mapping(target = "clinicId", source = "clinic.id")
    PatientResponseDTO toDto(Patient entity);
    // Do DTO para a Entidade
    // id do DTO vai usar qual da Entidade (NÃO TEM NÃO PRECISA PASSAR)
    // o clinic dO dto vai usar qual da Entidade  -> Vai usar o clinic.id
    // o name do DTO vai usar qual da Entidde (será convertido automaticamente pois são iguais)
}
