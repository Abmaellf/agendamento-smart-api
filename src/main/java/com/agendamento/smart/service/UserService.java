package com.agendamento.smart.service;

import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;

    public boolean existsByLogin(String login) {
       User usr =  (User) userRepository.findByLogin(login);
        return usr != null;
    }

}
