package com.agendamento.smart.service;

import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;

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
}
