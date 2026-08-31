package com.maxime.help.msauth.domain.port.out;

/**
 * Outbound port for hashing opaque tokens (e.g. refresh tokens) for storage/lookup. Unlike
 * {@link PasswordHasher}, this must be deterministic — the same input always maps to the same
 * hash — since it backs {@code RefreshTokenRepository.findByTokenHash}. Not suitable for
 * passwords: no salt, not intentionally slow. Safe here because the hashed values already carry
 * high entropy (randomly generated), so a slow salted hash buys nothing.
 */
public interface TokenHasher {

    String hash(String rawValue);
}
