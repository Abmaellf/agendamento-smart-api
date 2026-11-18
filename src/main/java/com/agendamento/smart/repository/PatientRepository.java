package com.agendamento.smart.repository;

import com.agendamento.smart.model.patient.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    @Query(
            value = """
                    select p
                    from Patient p
                    where p.clinic.id = :clinicId
                      and (
                         cast(:name as string) is null
                        or lower(p.name) like lower(concat('%', cast(:name as text), '%'))
                      )
                    """,
            countQuery = """
                    select count(p)
                    from Patient p
                    where p.clinic.id = :clinicId
                      and (
                         cast(:name as string) is null
                        or lower(p.name) like lower(concat('%', cast(:name as text), '%'))
                      )
                    """)
    Page<Patient> findPageByClinicIdAndName(
            @Param("clinicId") UUID clinicId,
            @Param("name") String name,
            Pageable pageable);

    @Query(
            value = """
                    select distinct p
                    from Patient p
                    join p.units unit
                    where p.clinic.id = :clinicId
                      and unit.id = :unitId
                      and (
                         cast(:name as string) is null
                        or lower(p.name) like lower(concat('%', cast(:name as text), '%'))
                      )
                    """,
            countQuery = """
                    select count(distinct p.id)
                    from Patient p
                    join p.units unit
                    where p.clinic.id = :clinicId
                      and unit.id = :unitId
                      and (
                         cast(:name as string) is null
                        or lower(p.name) like lower(concat('%', cast(:name as text), '%'))
                      )
                    """)
    Page<Patient> findPageByClinicIdAndUnitIdAndName(
            @Param("clinicId") UUID clinicId,
            @Param("unitId") UUID unitId,
            @Param("name") String name,
            Pageable pageable);

    @Query("select max(p.code) from Patient p")
    Optional<Long> findMaxCode();

    Optional<Patient> findByName(String name);

    List<Patient> findAllByClinicId(UUID clinicId);

    @Query("""
            select distinct p
            from Patient p
            join p.units unit
            where p.clinic.id = :clinicId
              and unit.id = :unitId
            """)
    List<Patient> findAllByClinicIdAndUnitId(
            @Param("clinicId") UUID clinicId,
            @Param("unitId") UUID unitId);

    Optional<Patient> findByIdAndClinicId(UUID id, UUID clinicId);

    @Query("""
            select distinct p
            from Patient p
            left join fetch p.units
            where p.id = :id
              and p.clinic.id = :clinicId
            """)
    Optional<Patient> findByIdAndClinicIdWithUnits(
            @Param("id") UUID id,
            @Param("clinicId") UUID clinicId);

    @Query("""
            select distinct p
            from Patient p
            left join fetch p.units
            where p.id = :id
            """)
    Optional<Patient> findByIdWithUnits(@Param("id") UUID id);
}
