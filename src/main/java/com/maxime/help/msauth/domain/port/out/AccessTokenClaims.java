package com.maxime.help.msauth.domain.port.out;

import com.maxime.help.msauth.domain.model.Role;
import java.util.UUID;

/** The claims carried by a verified access token, as returned by {@link AccessTokenIssuer#parse}. */
public record AccessTokenClaims(UUID userId, String email, Role role) {}
