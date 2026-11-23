package com.agendamento.smart.repository;

import com.agendamento.smart.model.session.AuthRefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AuthRefreshToken t join fetch t.session s join fetch s.user where t.tokenHash = :tokenHash")
    Optional<AuthRefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update AuthRefreshToken t set t.revokedAt = :revokedAt where t.session.id = :sessionId and t.revokedAt is null")
    int revokeAllBySessionId(@Param("sessionId") UUID sessionId, @Param("revokedAt") Instant revokedAt);
}
