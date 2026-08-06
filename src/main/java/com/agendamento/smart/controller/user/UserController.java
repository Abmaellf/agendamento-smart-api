package com.agendamento.smart.controller.user;

import com.agendamento.smart.dtos.user.RegisterDTO;
import com.agendamento.smart.dtos.user.UserResponseDTO;
import com.agendamento.smart.mapper.UserMapper;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("user")
@AllArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final UserMapper userMapper;
    private final UserService userService;

    @CrossOrigin(origins = "https://agendamentos-smart.vercel.app")
    @PostMapping("/register/{clinicId}")
    public ResponseEntity<UserResponseDTO> createUser(@PathVariable UUID clinicId, @RequestBody @Valid RegisterDTO data) {
        if (userService.existsByLogin(data.login())) {
            throw new IllegalArgumentException("Email já está em uso");
        }

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new RuntimeException("Clinic not found with id: " + clinicId));

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User user = userMapper.toEntity(data, clinic);
        user.setLogin(data.login());
        user.setPassword(encryptedPassword);
        user.setRole(data.role());
        user.setClinic(clinic);
        System.out.println("Register");
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
