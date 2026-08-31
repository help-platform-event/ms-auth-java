package com.maxime.help.msauth.infrastructure.security;

import com.maxime.help.msauth.domain.model.Role;
import com.maxime.help.msauth.domain.port.out.AccessTokenClaims;
import com.maxime.help.msauth.domain.port.out.AccessTokenIssuer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Signs and verifies access tokens as HS256 JWTs carrying sub/email/role claims. */
@Component
class JwtAccessTokenAdapter implements AccessTokenIssuer {

    private final SecretKey key;
    private final Duration accessTokenTtl;
    private final Clock clock;

    JwtAccessTokenAdapter(AuthProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.jwt().secret()));
        this.accessTokenTtl = properties.jwt().accessTokenTtl();
        this.clock = clock;
    }

    @Override
    public String issue(UUID userId, String email, Role role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Optional<AccessTokenClaims> parse(String token) {
        try {
            // jjwt's own expiration check uses its parser Clock, not java.time.Clock - wire the
            // same injected clock through so expiry is deterministic under tests.
            Claims claims =
                    Jwts.parser()
                            .verifyWith(key)
                            .clock(() -> Date.from(clock.instant()))
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
            return Optional.of(
                    new AccessTokenClaims(
                            UUID.fromString(claims.getSubject()),
                            claims.get("email", String.class),
                            Role.valueOf(claims.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
