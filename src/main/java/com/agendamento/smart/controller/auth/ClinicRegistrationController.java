package com.agendamento.smart.controller.auth;

import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.service.ClinicOnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/register")
@RequiredArgsConstructor
public class ClinicRegistrationController {

    private final ClinicOnboardingService onboardingService;

    @PostMapping("/clinic")
    public ResponseEntity<ClinicRegistrationResponse> registerClinic(
            @RequestBody @Valid ClinicRegistrationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingService.register(request));
    }
}
