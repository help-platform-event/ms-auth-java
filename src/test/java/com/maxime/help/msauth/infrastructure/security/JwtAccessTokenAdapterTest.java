package com.maxime.help.msauth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxime.help.msauth.domain.model.Role;
import com.maxime.help.msauth.domain.port.out.AccessTokenClaims;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtAccessTokenAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String SECRET =
            Base64.getEncoder()
                    .encodeToString("test-only-secret-key-32-bytes-min".getBytes(StandardCharsets.UTF_8));

    private static JwtAccessTokenAdapter adapterAt(Instant instant) {
        AuthProperties properties =
                new AuthProperties(
                        new AuthProperties.Jwt(SECRET, Duration.ofMinutes(15)),
                        new AuthProperties.RefreshToken(Duration.ofDays(30)),
                        new AuthProperties.Google("postmessage"));
        return new JwtAccessTokenAdapter(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void issueThenParse_roundTripsClaims() {
        JwtAccessTokenAdapter adapter = adapterAt(NOW);
        UUID userId = UUID.randomUUID();

        String token = adapter.issue(userId, "alice@example.com", Role.USER);
        Optional<AccessTokenClaims> claims = adapter.parse(token);

        assertThat(claims).contains(new AccessTokenClaims(userId, "alice@example.com", Role.USER));
    }

    @Test
    void parse_returnsEmptyForTamperedToken() {
        JwtAccessTokenAdapter adapter = adapterAt(NOW);
        String token = adapter.issue(UUID.randomUUID(), "alice@example.com", Role.USER);

        assertThat(adapter.parse(token + "tampered")).isEmpty();
    }

    @Test
    void parse_returnsEmptyForExpiredToken() {
        JwtAccessTokenAdapter issuer = adapterAt(NOW);
        String token = issuer.issue(UUID.randomUUID(), "alice@example.com", Role.USER);

        JwtAccessTokenAdapter laterVerifier = adapterAt(NOW.plus(Duration.ofMinutes(16)));

        assertThat(laterVerifier.parse(token)).isEmpty();
    }
}
