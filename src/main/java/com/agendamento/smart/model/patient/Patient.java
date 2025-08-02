
package com.agendamento.smart.model.patient;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Random;

@Table(name="PATIENT")
@Entity
@Getter
@Setter
@AllArgsConstructor
//@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private final Random code;
    private String name;
    private LocalDate createdAt = LocalDate.now();

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
        return code.nextInt(0,999999999);
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