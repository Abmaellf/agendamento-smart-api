
package com.agendamento.smart.model.patient;

import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.util.CodeGeneratorListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "PATIENT")
@EntityListeners(CodeGeneratorListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private Long code;

    @Column(nullable = false)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "PATIENT_UNIT",
            joinColumns = @JoinColumn(name = "patient_id"),
            inverseJoinColumns = @JoinColumn(name = "unit_id")
    )
    private Set<ClinicUnit> units = new LinkedHashSet<>();

}
