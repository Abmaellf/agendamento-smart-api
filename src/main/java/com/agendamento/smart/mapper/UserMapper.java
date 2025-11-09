package com.agendamento.smart.mapper;

import com.agendamento.smart.dtos.AuthenticationDTO;
import com.agendamento.smart.dtos.user.RegisterDTO;
import com.agendamento.smart.dtos.user.UserResponseDTO;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

//     UserResponseDTO toEntity(RegisterDTO registerDTO, Clinic clinic);

     User toEntity(RegisterDTO registerDTO, Clinic clinic);
}
