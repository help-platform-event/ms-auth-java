package com.maxime.help.msauth.domain.port.out;

import com.maxime.help.msauth.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for refresh-token persistence. Implemented in {@code infrastructure.persistence}. */
public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Removes every token of a user — building block for single-session enforcement. */
    void deleteByUserId(UUID userId);
}
