package com.agendamento.smart.controller.catalog;

import com.agendamento.smart.dtos.catalog.CatalogResponse;
import com.agendamento.smart.dtos.catalog.PatientOptionResponse;
import com.agendamento.smart.dtos.catalog.ProfessionalOptionResponse;
import com.agendamento.smart.dtos.catalog.ServiceOptionResponse;
import com.agendamento.smart.dtos.catalog.UnitOptionResponse;
import com.agendamento.smart.dtos.catalog.UpdateServiceRequest;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/patients")
    public CatalogResponse<PatientOptionResponse> patients(@AuthenticationPrincipal User user) {
        return new CatalogResponse<>(catalogService.patients(user));
    }

    @GetMapping("/units")
    public CatalogResponse<UnitOptionResponse> units(@AuthenticationPrincipal User user) {
        return new CatalogResponse<>(catalogService.units(user));
    }

    @GetMapping("/services")
    public CatalogResponse<ServiceOptionResponse> services(@AuthenticationPrincipal User user) {
        return new CatalogResponse<>(catalogService.services(user));
    }

    @GetMapping("/professionals")
    public CatalogResponse<ProfessionalOptionResponse> professionals(
            @AuthenticationPrincipal User user,
            @RequestParam UUID serviceId) {
        return new CatalogResponse<>(catalogService.professionals(user, serviceId));
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceOptionResponse> updateService(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(catalogService.updateService(user, id, request));
    }
}
