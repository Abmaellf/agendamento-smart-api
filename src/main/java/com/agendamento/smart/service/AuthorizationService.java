package com.agendamento.smart.service;

import com.agendamento.smart.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

//@RequiredArgsConstructor ver a possibilidade de utilização  https://www.linkedin.com/posts/leonardo-tavares-b024b1348_springboot-java-anotaaexaeles-activity-7366481243649040384-ceYG?utm_source=share&utm_medium=member_desktop&rcm=ACoAABJkljUByoGYKb3ZmgTWsUHyVjcbNBWaohk
@Service
public class AuthorizationService implements UserDetailsService {

    @Autowired
    UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByLogin(username);
    }
}
