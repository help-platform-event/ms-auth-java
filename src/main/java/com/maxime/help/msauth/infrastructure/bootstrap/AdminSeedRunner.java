package com.maxime.help.msauth.infrastructure.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.maxime.help.msauth.application.service.AdminProvisioningResult;
import com.maxime.help.msauth.application.service.AdminProvisioningService;

/**
 * Seeds the administrator account at startup from {@code ADMIN_EMAIL}/{@code ADMIN_PASSWORD}.
 * Disabled when neither is set (the dev default); any failure aborts startup, so a misconfigured
 * deploy never comes up without an admin.
 */
@Component
class AdminSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedRunner.class);

    private final AdminSeedProperties properties;
    private final AdminProvisioningService adminProvisioningService;

    AdminSeedRunner(AdminSeedProperties properties, AdminProvisioningService adminProvisioningService) {
        this.properties = properties;
        this.adminProvisioningService = adminProvisioningService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.isBlank() && properties.required()) {
            throw new IllegalStateException(
                    "Admin seed is required (prod profile): set ADMIN_EMAIL and ADMIN_PASSWORD");
        }
        if (properties.isBlank()) {
            log.info("Admin seed disabled (app.seed.admin.email/password not set)");
            return;
        }
        if (!properties.isComplete()) {
            throw new IllegalStateException(
                    "Admin seed misconfigured: app.seed.admin.email and app.seed.admin.password must be set together");
        }
        AdminProvisioningResult result =
                adminProvisioningService.ensureAdminExists(properties.email(), properties.password());
        switch (result) {
            case CREATED -> log.info("Admin seed: created administrator {}", properties.email());
            case ALREADY_PRESENT -> log.info("Admin seed: an administrator already exists, nothing to do");
        }
    }
}
