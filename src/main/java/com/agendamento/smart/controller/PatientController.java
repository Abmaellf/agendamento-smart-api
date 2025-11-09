package com.agendamento.smart.controller;

import com.agendamento.smart.dtos.PageResponseDTO;
import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//@CrossOrigin(origins = "https://agendamentos-smart.vercel.app/**")
@RestController
@RequestMapping("patient")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @GetMapping("/list")
    public ResponseEntity<PageResponseDTO<PatientResponseDTO>> findAllPatient(Pageable pageable) {
        PageResponseDTO<PatientResponseDTO> patientResponseDTO = patientService.findAllPatient(pageable);
        return ResponseEntity.ok().body(patientResponseDTO);
    }

    @PostMapping("/save")
    public ResponseEntity<PatientResponseDTO> savePatient(@RequestBody @Valid PatientRequestDTO dto){
        PatientResponseDTO response = patientService.save(dto);
        System.out.println(response);
        return ResponseEntity.ok(response);
    }
}
