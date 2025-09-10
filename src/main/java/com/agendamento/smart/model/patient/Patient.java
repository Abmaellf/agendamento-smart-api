
package com.agendamento.smart.model.patient;

import com.agendamento.smart.model.clinic.Clinic;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

@Table(name="PATIENT")
@Entity
@Getter
@Setter
@AllArgsConstructor
//@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Patient implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private final Random code;
    private String name;

    @ManyToOne
    @JoinColumn(name = "clinic_id") //nome no bd
    private Clinic clinic;

    private LocalDate createdAt = LocalDate.now();


    public Clinic getClinic() {
        return clinic;
    }

    public void setClinic(Clinic clinic) {
        this.clinic = clinic;
    }

    public Patient() {
        this.code = new Random();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCode() {
        return code.nextInt(0,999999);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

}