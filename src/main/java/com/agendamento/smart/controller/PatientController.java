package com.agendamento.smart.controller;

import com.agendamento.smart.dtos.patient.PatientRequestDTO;
import com.agendamento.smart.dtos.patient.PatientResponseDTO;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@CrossOrigin(origins = "https://agendamentos-smart.vercel.app/**")
@RestController
@RequestMapping("patient")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @GetMapping("/list")
    public ResponseEntity<List<Patient>> findAllPatient() {
        return ResponseEntity.ok().body(patientService.findAllPatient());
    }

    @PostMapping("/save")
    public ResponseEntity savePatient(@RequestBody @Valid PatientRequestDTO dto){
        PatientResponseDTO response = patientService.save(dto);
//        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.save(data));
        return ResponseEntity.ok(response);
    }
}
