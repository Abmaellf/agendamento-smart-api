package com.agendamento.smart.controller.scheduling;
import com.agendamento.smart.dtos.agendamento.SchedulingRequest;
import com.agendamento.smart.dtos.agendamento.SchedulingResponse;
import com.agendamento.smart.service.scheduling.SchedulingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/scheduling")
public class SchedulingController {

    private final SchedulingService service;

    public SchedulingController(SchedulingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SchedulingResponse> create(@Valid @RequestBody SchedulingRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SchedulingResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
