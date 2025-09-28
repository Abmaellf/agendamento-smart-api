package com.agendamento.smart.mapper;

import com.agendamento.smart.dtos.user.RegisterDTO;
import com.agendamento.smart.dtos.user.UserResponseDTO;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.user.User;

public class UserMapper {
    public static User toEntity(RegisterDTO dto, Clinic clinic){
        User user = new User();
        user.setLogin(dto.login());
        user.setPassword(dto.password());
        user.setRole(dto.role());
        user.setClinic(clinic);
        return user;
    }

    public static UserResponseDTO toDTO(User user){
        return new UserResponseDTO(
                user.getLogin(),
                user.getRole(),
                user.getClinic() != null ? user.getClinic().getId() : null,
                user.getClinic() != null ? user.getClinic().getName() : null

        );
    }
}
