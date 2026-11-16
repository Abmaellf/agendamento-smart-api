package com.agendamento.smart.model.packageappointment;

import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.servicepackage.ServicePackageDefinition;
import com.agendamento.smart.model.servicepackage.PackageWeekDay;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity @Table(name = "PACKAGE_CONTRACT", uniqueConstraints = @UniqueConstraint(name = "uk_package_contract_idempotency", columnNames = {"clinic_id", "idempotency_key"}))
@Getter @Setter @NoArgsConstructor
public class PackageAppointmentContract {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "clinic_id") private Clinic clinic;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "package_id") private ServicePackageDefinition packageDefinition;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id") private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unit_id") private ClinicUnit unit;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "service_id") private ServiceOffering service;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by") private User createdBy;
    @Column(name = "total_sessions", nullable = false) private Integer totalSessions;
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2) private BigDecimal totalPrice;
    @Column(name = "requested_start_date", nullable = false) private LocalDate requestedStartDate;
    @Column(name = "appointment_time", nullable = false) private LocalTime appointmentTime;
    @Column(name = "effective_first_date", nullable = false) private LocalDate effectiveFirstDate;
    @Column(name = "effective_last_date", nullable = false) private LocalDate effectiveLastDate;
    @Column(name = "idempotency_key", nullable = false, length = 120) private String idempotencyKey;
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "PACKAGE_CONTRACT_WEEK_DAY", joinColumns = @JoinColumn(name = "contract_id")) @Enumerated(EnumType.STRING) @Column(name = "week_day", nullable = false) private Set<PackageWeekDay> effectiveWeekDays = new LinkedHashSet<>();
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
