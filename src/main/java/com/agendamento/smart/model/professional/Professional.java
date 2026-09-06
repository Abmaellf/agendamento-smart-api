package com.agendamento.smart.model.professional;

import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "PROFESSIONAL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Professional {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(nullable = false)
    private String name;

    @Column(name = "available_week_days", nullable = false)
    private String availableWeekDaysValue;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "PROFESSIONAL_SERVICE",
            joinColumns = @JoinColumn(name = "professional_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private Set<ServiceOffering> services;

    public List<Integer> availableWeekDays() {
        return Arrays.stream(availableWeekDaysValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Integer::valueOf)
                .toList();
    }

    public boolean offers(UUID serviceId) {
        return services.stream().anyMatch(service -> service.getId().equals(serviceId));
    }
}
