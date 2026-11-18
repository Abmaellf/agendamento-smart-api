package com.agendamento.smart.controller.patient;

import com.agendamento.smart.dtos.PageResponseDTO;
import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.service.PatientServiceInt;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientServiceInt patientService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<PageResponseDTO<PatientResponseDTO>> findAllPatient(
            @PageableDefault(
                    size = 5,
                    sort = "code",
                    direction = Sort.Direction.ASC)
            Pageable pageable,
            @RequestParam(required = false) String name,
            Authentication authentication) {
        PageResponseDTO<PatientResponseDTO> response =
                patientService.findAllPatient(pageable, name, authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<PatientResponseDTO> savePatient(
            @RequestBody @Valid PatientRequestDTO dto,
            Authentication authentication) {
        PatientResponseDTO response = patientService.save(dto, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
