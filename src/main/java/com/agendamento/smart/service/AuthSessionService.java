package com.agendamento.smart.service;

import com.agendamento.smart.infra.security.TokenService;
import com.agendamento.smart.model.session.AuthRefreshToken;
import com.agendamento.smart.model.session.AuthSession;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.AuthRefreshTokenRepository;
import com.agendamento.smart.repository.AuthSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final AuthSessionRepository sessionRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final Clock clock = Clock.systemUTC();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${api.security.token.refresh-ttl:30d}")
    private Duration refreshTokenTtl;

    @Transactional
    public SessionTokens create(User user) {
        Instant now = clock.instant();
        AuthSession session = sessionRepository.saveAndFlush(AuthSession.builder()
                .user(user)
                .createdAt(now)
                .expiresAt(now.plus(refreshTokenTtl))
                .build());

        String refreshToken = newRefreshToken();
        refreshTokenRepository.save(AuthRefreshToken.builder()
                .session(session)
                .tokenHash(hash(refreshToken))
                .createdAt(now)
                .expiresAt(session.getExpiresAt())
                .build());

        return new SessionTokens(
                tokenService.generateAccessToken(user, session.getId()),
                refreshToken,
                session.getExpiresAt());
    }

    @Transactional
    public SessionTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidSessionException();
        }
        Instant now = clock.instant();
        AuthRefreshToken current = refreshTokenRepository
                .findByTokenHashForUpdate(hash(rawRefreshToken))
                .orElseThrow(InvalidSessionException::new);

        if (!current.isUsable(now)) {
            revoke(current.getSession(), now);
            throw new InvalidSessionException();
        }

        current.setUsedAt(now);
        refreshTokenRepository.save(current);

        String replacement = newRefreshToken();
        refreshTokenRepository.save(AuthRefreshToken.builder()
                .session(current.getSession())
                .tokenHash(hash(replacement))
                .createdAt(now)
                .expiresAt(current.getSession().getExpiresAt())
                .build());

        return new SessionTokens(
                tokenService.generateAccessToken(current.getSession().getUser(), current.getSession().getId()),
                replacement,
                current.getSession().getExpiresAt());
    }

    @Transactional(readOnly = true)
    public boolean isActive(UUID sessionId, String login) {
        return sessionRepository.findWithUserById(sessionId)
                .filter(session -> session.isActive(clock.instant()))
                .filter(session -> session.getUser().getLogin().equalsIgnoreCase(login))
                .isPresent();
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHashForUpdate(hash(rawRefreshToken))
                .ifPresent(token -> revoke(token.getSession(), clock.instant()));
    }

    private void revoke(AuthSession session, Instant now) {
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(now);
            sessionRepository.save(session);
        }
        refreshTokenRepository.revokeAllBySessionId(session.getId(), now);
    }

    private String newRefreshToken() {
        byte[] value = new byte[48];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível", exception);
        }
    }

    public record SessionTokens(String accessToken, String refreshToken, Instant sessionExpiresAt) {
    }

    public static class InvalidSessionException extends RuntimeException {
    }
}
