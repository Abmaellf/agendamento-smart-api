package com.agendamento.smart.model.appointment;

import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.professional.Professional;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Table(
        name = "APPOINTMENT",
        indexes = {
                @Index(
                        name = "idx_appointment_clinic_start",
                        columnList = "clinic_id, starts_at"
                ),
                @Index(
                        name = "idx_appointment_unit_start",
                        columnList = "clinic_id, unit_id, starts_at"
                ),
                @Index(
                        name = "idx_appointment_professional_start",
                        columnList = "clinic_id, professional_id, starts_at"
                ),
                @Index(
                        name = "idx_appointment_patient_start",
                        columnList = "clinic_id, patient_id, starts_at"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_appointment_clinic_idempotency",
                        columnNames = {"clinic_id", "idempotency_key"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Appointment {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ClinicUnit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "professional_id", nullable = true)
    private Professional professional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "time_zone")
    private String timeZone;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "idempotency_key", length = 120, updatable = false)
    private String idempotencyKey;

    @Column(columnDefinition = "json", nullable = false)
    @Convert(converter = StringListJsonConverter.class)
    private List<String> pathology;

    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;

    @Column(nullable = false)
    private LocalTime hours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.AGENDADO;

    private String variant;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
