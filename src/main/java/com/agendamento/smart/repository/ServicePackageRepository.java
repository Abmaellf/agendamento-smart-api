package com.agendamento.smart.repository;

import com.agendamento.smart.model.servicepackage.ServicePackageDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface ServicePackageRepository extends JpaRepository<ServicePackageDefinition, UUID> {
    @Query("""
            select item from ServicePackageDefinition item
            where item.clinic.id = :clinicId
              and (:unitId is null or item.unit.id = :unitId)
              and (:serviceId is null or item.service.id = :serviceId)
              and (:active is null or item.active = :active)
              and (:totalSessions is null or item.totalSessions = :totalSessions)
              and (coalesce(:name, '') = '' or lower(item.name) like lower(concat('%', coalesce(:name, ''), '%')))
            """)
    Page<ServicePackageDefinition> findPageByClinicIdAndFilters(
            @Param("clinicId") UUID clinicId,
            @Param("unitId") UUID unitId,
            @Param("serviceId") UUID serviceId,
            @Param("active") Boolean active,
            @Param("totalSessions") Integer totalSessions,
            @Param("name") String name,
            Pageable pageable);

    Optional<ServicePackageDefinition> findByIdAndClinicId(UUID id, UUID clinicId);
}
