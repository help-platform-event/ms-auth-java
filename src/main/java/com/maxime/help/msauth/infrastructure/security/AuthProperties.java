package com.maxime.help.msauth.infrastructure.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds {@code app.auth.*} configuration. Picked up via {@code @ConfigurationPropertiesScan}.
 * Validated so that a missing JWT secret (empty under the prod profile) aborts startup with a clear
 * message rather than a Base64 decoding error deep inside jjwt.
 */
@ConfigurationProperties(prefix = "app.auth")
@Validated
record AuthProperties(@Valid Jwt jwt, RefreshToken refreshToken, Google google) {

    record Jwt(@NotBlank String secret, Duration accessTokenTtl) {}

    record RefreshToken(Duration ttl) {}

    record Google(String redirectUri) {}
}
