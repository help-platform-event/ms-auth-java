package com.maxime.help.msauth.domain.port.out;

/**
 * A Google identity resolved from a verified id_token. {@code emailVerified} must be checked
 * before using {@code email} to match against an existing local account — an unverified email
 * claim must never be trusted to link identities.
 */
public record GoogleIdentity(String sub, String email, boolean emailVerified) {}
