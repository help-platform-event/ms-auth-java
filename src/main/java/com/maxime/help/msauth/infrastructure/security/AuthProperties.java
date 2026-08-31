package com.maxime.help.msauth.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code app.auth.*} configuration. Picked up via {@code @ConfigurationPropertiesScan}. */
@ConfigurationProperties(prefix = "app.auth")
record AuthProperties(Jwt jwt, RefreshToken refreshToken, Google google) {

    record Jwt(String secret, Duration accessTokenTtl) {}

    record RefreshToken(Duration ttl) {}

    record Google(String redirectUri) {}
}
