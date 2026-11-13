package com.agendamento.smart.model.servicepackage;

import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity @Table(name = "SERVICE_PACKAGE") @Getter @Setter @NoArgsConstructor
public class ServicePackageDefinition {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "clinic_id") private Clinic clinic;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unit_id") private ClinicUnit unit;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "service_id") private ServiceOffering service;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by") private User createdBy;
    @Column(nullable = false) private String name;
    @Column(length = 2000) private String description;
    @Column(name = "total_sessions", nullable = false) private Integer totalSessions;
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2) private BigDecimal totalPrice;
    @Column(nullable = false) private boolean active = true;
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "SERVICE_PACKAGE_WEEK_DAY", joinColumns = @JoinColumn(name = "service_package_id")) @Enumerated(EnumType.STRING) @Column(name = "week_day", nullable = false)
    private Set<PackageWeekDay> weekDays = new LinkedHashSet<>();
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private Long version;
}
