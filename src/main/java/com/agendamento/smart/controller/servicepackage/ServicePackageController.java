package com.agendamento.smart.controller.servicepackage;

import com.agendamento.smart.dtos.catalog.CatalogResponse;
import com.agendamento.smart.dtos.servicepackage.ServicePackageRequest;
import com.agendamento.smart.dtos.servicepackage.ServicePackageResponse;
import com.agendamento.smart.dtos.servicepackage.ServicePackageStatusRequest;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.service.ServicePackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/service-packages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ServicePackageController {
    private final ServicePackageService service;

    @PostMapping
    public ResponseEntity<ServicePackageResponse> create(@AuthenticationPrincipal User user, @Valid @RequestBody ServicePackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user, request));
    }

    @GetMapping
    public CatalogResponse<ServicePackageResponse> findAll(@AuthenticationPrincipal User user,
            @RequestParam(required = false) UUID unitId, @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) Boolean active, @RequestParam(required = false) Integer totalSessions,
            @RequestParam(required = false) String name) {
        return new CatalogResponse<>(service.findAll(user, unitId, serviceId, active, totalSessions, name));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicePackageResponse> findById(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(user, id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServicePackageResponse> update(@AuthenticationPrincipal User user, @PathVariable UUID id, @Valid @RequestBody ServicePackageRequest request) {
        return ResponseEntity.ok(service.update(user, id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ServicePackageResponse> updateStatus(@AuthenticationPrincipal User user, @PathVariable UUID id, @Valid @RequestBody ServicePackageStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(user, id, request.active()));
    }
}
