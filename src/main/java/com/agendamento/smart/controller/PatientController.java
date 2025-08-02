package com.agendamento.smart.controller;

import com.agendamento.smart.dtos.PatientDTO;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity savePatient(@RequestBody @Valid PatientDTO data){
        Patient newPatient = new Patient();
        newPatient.setName(data.name());
        this.patientService.save(newPatient);
        return ResponseEntity.ok().build();
    }
}
