package com.agendamento.smart.controller.appointment;

import com.agendamento.smart.dtos.appointment.AppointmentRequest;
import com.agendamento.smart.dtos.appointment.AppointmentResponse;
import com.agendamento.smart.dtos.catalog.CatalogResponse;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.service.appointment.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            @Valid @RequestBody AppointmentRequest request,
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, user, idempotencyKey));
    }

    @GetMapping
    public CatalogResponse<AppointmentResponse> findAll(
            @AuthenticationPrincipal User user,
            @RequestParam UUID unitId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return new CatalogResponse<>(service.findAll(user, unitId, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.findById(id, user));
    }
}
