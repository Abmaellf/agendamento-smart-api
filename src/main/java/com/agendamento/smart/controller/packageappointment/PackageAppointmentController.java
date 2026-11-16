package com.agendamento.smart.controller.packageappointment;

import com.agendamento.smart.dtos.packageappointment.*;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.service.packageappointment.PackageAppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/package-appointments") @RequiredArgsConstructor
public class PackageAppointmentController {
    private final PackageAppointmentService service;
    @PostMapping("/preview") public PackageAppointmentPreviewResponse preview(@Valid @RequestBody PackageAppointmentRequest request, @AuthenticationPrincipal User user) { return service.preview(request, user); }
    @PostMapping("/confirm") public ResponseEntity<PackageAppointmentConfirmationResponse> confirm(@Valid @RequestBody PackageAppointmentRequest request, @AuthenticationPrincipal User user, @RequestHeader("Idempotency-Key") String key, @RequestHeader(value="X-Preview-Fingerprint", required=false) String fingerprint) { return ResponseEntity.status(HttpStatus.CREATED).body(service.confirm(request, user, key, fingerprint)); }
    @GetMapping("/{id}") public PackageAppointmentConfirmationResponse findById(@PathVariable UUID id, @AuthenticationPrincipal User user) { return service.findById(id, user); }
}
