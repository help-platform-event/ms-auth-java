package com.maxime.help.msauth.application.service;

/** Outcome of {@link AdminProvisioningService#ensureAdminExists}. */
public enum AdminProvisioningResult {
    CREATED,
    ALREADY_PRESENT
}
