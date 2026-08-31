package com.maxime.help.msauth.domain.port.out;

/**
 * Outbound port for user password hashing. Salted and deliberately slow — never use for values
 * that need deterministic lookup (see {@link TokenHasher} for that case).
 */
public interface PasswordHasher {

    /** Hashes a raw password. Non-deterministic: the same input yields a different hash each call. */
    String hash(String rawPassword);

    /** Verifies a raw password against a previously produced hash. */
    boolean matches(String rawPassword, String hash);
}
