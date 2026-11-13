package com.agendamento.smart.repository;

import com.agendamento.smart.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @Query("select u from User u where lower(u.login) = lower(:login)")
    UserDetails findByLogin(@Param("login") String login);

    boolean existsByLoginIgnoreCase(String login);

    long countByClinicId(UUID clinicId);

    long countByClinicIdAndRole(UUID clinicId, com.agendamento.smart.model.user.UserRole role);

    List<User> findAllByClinicId(UUID clinicId);

    List<User> findAllByClinicIdAndClinicUnitId(UUID clinicId, UUID clinicUnitId);
}
