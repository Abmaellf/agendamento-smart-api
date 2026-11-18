package com.agendamento.smart.mapper;

import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.dtos.patient.PrincipalServiceResponse;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.unit.ClinicUnit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    /**
     * Converte o request em entidade sem confiar nos relacionamentos enviados
     * pelo cliente. Clínica e unidades são resolvidas pelo PatientService.
     */
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "units", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Patient toEntity(PatientRequestDTO dto);

    /**
     * Mapeamento usado quando status e serviço principal não foram resolvidos.
     * O retorno é seguro para a listagem porque não inventa informações.
     */
    default PatientResponseDTO toDto(Patient entity) {
        return toDto(entity, null, null);
    }

    /**
     * Mapeamento completo. O service deve fornecer status e serviço principal
     * somente quando existir uma fonte confiável para esses dados.
     */
    default PatientResponseDTO toDto(
            Patient entity,
            String status,
            PrincipalServiceResponse principalService) {
        if (entity == null) {
            return null;
        }

        UUID clinicId = entity.getClinic() == null
                ? null
                : entity.getClinic().getId();

        return new PatientResponseDTO(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                clinicId,
                unitIds(entity),
                entity.getCreatedAt(),
                status,
                principalService
        );
    }

    /**
     * Converte as unidades associadas para IDs estáveis e determinísticos.
     * A ordenação evita respostas diferentes apenas pela ordem do Set.
     */
    default List<UUID> unitIds(Patient entity) {
        if (entity.getUnits() == null) {
            return List.of();
        }

        return entity.getUnits().stream()
                .map(ClinicUnit::getId)
                .sorted()
                .toList();
    }
}
