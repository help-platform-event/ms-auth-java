package com.maxime.help.msauth.domain.port.out;

import com.maxime.help.msauth.domain.model.Role;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for minting and verifying signed, self-contained access tokens. */
public interface AccessTokenIssuer {

    /** Mints a signed access token carrying the user id, email and role as claims. */
    String issue(UUID userId, String email, Role role);

    /** Parses and verifies a token; empty if missing, expired, tampered, or otherwise invalid. */
    Optional<AccessTokenClaims> parse(String token);
}
