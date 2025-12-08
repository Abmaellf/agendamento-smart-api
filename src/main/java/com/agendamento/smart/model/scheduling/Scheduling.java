package com.agendamento.smart.model.scheduling;

import com.agendamento.smart.model.patient.Patient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Table(name="SCHEDULING")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Scheduling {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(optional = false)  // FK patient_id
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(columnDefinition = "json", nullable = false)
    @Convert(converter = StringListJsonConverter.class)
    private List<String> pathology;

    @Column(name = "date_scheduling", nullable = false)
    private LocalDateTime dateScheduling;

    @Column(nullable = false)
    private LocalTime hours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusScheduling status = StatusScheduling.AGENDADO;

    private String variant;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}