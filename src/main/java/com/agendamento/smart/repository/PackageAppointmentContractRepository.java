package com.agendamento.smart.repository;

import com.agendamento.smart.model.packageappointment.PackageAppointmentContract;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PackageAppointmentContractRepository extends JpaRepository<PackageAppointmentContract, UUID> {
    Optional<PackageAppointmentContract> findByClinicIdAndIdempotencyKey(UUID clinicId, String idempotencyKey);
    Optional<PackageAppointmentContract> findByIdAndClinicId(UUID id, UUID clinicId);
}
