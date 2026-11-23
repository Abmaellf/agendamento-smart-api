package com.agendamento.smart.infra.security;

import com.agendamento.smart.model.user.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    String secret;

    @Value("${api.security.token.access-ttl:15m}")
    Duration accessTokenTtl;

    public String generateAccessToken(User user, UUID sessionId) {
        return createToken(user, sessionId, accessTokenTtl);
    }

    /**
     * Mantém compatibilidade com consumidores técnicos que ainda criam um
     * access token diretamente para o cabeçalho Authorization. Tokens de
     * sessão emitidos pelo login sempre usam generateAccessToken.
     */
    public String generateToken(User user) {
        return createToken(user, null, Duration.ofHours(2));
    }

    private String createToken(User user, UUID sessionId, Duration ttl) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var builder = JWT.create()
                    .withIssuer("agendamento-smart-api")
                    .withSubject(user.getUsername())
                    .withClaim("token_type", "access")
                    .withExpiresAt(Instant.now().plus(ttl));
            if (sessionId != null) {
                builder.withClaim("sid", sessionId.toString());
            }
            return builder.sign(algorithm);
        } catch (JWTCreationException ex) {
            throw new RuntimeException("Error while generating token", ex);
        }
    }

    public TokenClaims parse(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT decoded = JWT.require(algorithm)
                    .withIssuer("agendamento-smart-api")
                    .build()
                    .verify(token);
            String tokenType = decoded.getClaim("token_type").asString();
            if (tokenType != null && !"access".equals(tokenType)) {
                return null;
            }
            String sessionId = decoded.getClaim("sid").asString();
            return new TokenClaims(
                    decoded.getSubject(),
                    sessionId == null ? null : UUID.fromString(sessionId));
        } catch (JWTVerificationException | IllegalArgumentException exception) {
            return null;
        }
    }

    public record TokenClaims(String subject, UUID sessionId) {
    }
}
