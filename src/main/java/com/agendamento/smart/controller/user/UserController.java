package com.agendamento.smart.controller.user;

import com.agendamento.smart.dtos.user.RegisterDTO;
import com.agendamento.smart.service.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserController {

    private final UserRegistrationService userRegistrationService;

    @CrossOrigin(origins = "https://agendamentos-smart.vercel.app")
    @PostMapping("/register/{clinicId}")
    public ResponseEntity<Void> createUser(
            @PathVariable UUID clinicId,
            @RequestBody @Valid RegisterDTO data,
            Authentication authentication
    ) {
        userRegistrationService.register(clinicId, data, authentication);
        return ResponseEntity.ok().build();
    }
}
