package com.agendamento.smart.mapper;
import com.agendamento.smart.dtos.agendamento.SchedulingRequest;
import com.agendamento.smart.dtos.agendamento.SchedulingResponse;
import com.agendamento.smart.model.scheduling.Scheduling;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SchedulingMapper {

    @Mapping(target = "id", ignore = true)
    Scheduling toEntity(SchedulingRequest request);

    @Mapping(target = "patientId", source = "patient.id")
    SchedulingResponse toResponse(Scheduling scheduling);
}
