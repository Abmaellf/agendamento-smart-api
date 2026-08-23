package com.agendamento.smart.service;

import com.agendamento.smart.controller.exception.ResourceNotFoundException;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.dto.UserProfileResponse;
import com.agendamento.smart.repository.UnitRepository;
import com.agendamento.smart.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;

    @Autowired
    private UnitRepository unitRepository;

    public boolean existsByLogin(String login) {
       User usr =  (User) userRepository.findByLogin(login);
        return usr != null;
    }

    public User currentUserService(Authentication authentication) {

            authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getPrincipal() == null) {
                throw new AccessDeniedException("Usuário não autenticado");
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof User userDetails) {
                return userRepository.findById(userDetails.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
            }

            throw new AccessDeniedException("Tipo de principal não suportado");
        }

    public UserProfileResponse getUserProfile(Authentication authentication) {
        // 1. Busca o usuário
        User user = currentUserService(authentication);

        // 2. Busca a unidade (Lógica que estava no controller)
        var unit = unitRepository.findFirstByClinicIdOrderByNameAsc(user.getClinic().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade ativa não encontrada"));

        // 3. Define a Role
        String roleStr = user.getRole() == com.agendamento.smart.model.user.UserRole.USER ? "BASIC" : "ADMIN";

        // 4. Retorna o DTO seguro (Sem senha!)
        return new UserProfileResponse(
                user.getId(),
                user.getLogin(),
                user.getUsername(),
                user.getLogin(), // Seu código original usa user.getLogin() para o campo "name"
                roleStr,
                user.getClinic().getId(),
                unit.getTenantId(),
                unit.getId()
        );
    }
}
