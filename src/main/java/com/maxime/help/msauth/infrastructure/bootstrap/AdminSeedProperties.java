package com.maxime.help.msauth.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.seed.admin.*}. Picked up via {@code @ConfigurationPropertiesScan}. {@code
 * required} is set by the prod profile, where running without an admin seed is a misconfiguration.
 */
@ConfigurationProperties(prefix = "app.seed.admin")
record AdminSeedProperties(String email, String password, boolean required) {

    boolean isBlank() {
        return isBlank(email) && isBlank(password);
    }

    boolean isComplete() {
        return !isBlank(email) && !isBlank(password);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
