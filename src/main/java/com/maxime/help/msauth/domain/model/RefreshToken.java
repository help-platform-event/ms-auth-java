package com.maxime.help.msauth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A persisted refresh token — its own aggregate, referencing the owning user by id only.
 * {@code tokenHash} holds the hash of the opaque value handed to the client; the raw value is
 * never kept here.
 *
 * <p>{@code id} and {@code createdAt} are assigned by the persistence adapter.
 */
public class RefreshToken {

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt;

    private RefreshToken() {
    }

    /** Issues a fresh, non-revoked token for a user. */
    public static RefreshToken issue(UUID userId, String tokenHash, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.userId = Objects.requireNonNull(userId, "userId");
        token.tokenHash = requireText(tokenHash);
        token.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        token.revoked = false;
        return token;
    }

    /** Rebuilds a token from persisted state. Infrastructure use only. */
    public static RefreshToken reconstitute(
            UUID id,
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            boolean revoked,
            Instant createdAt) {
        RefreshToken token = new RefreshToken();
        token.id = id;
        token.userId = userId;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        token.revoked = revoked;
        token.createdAt = createdAt;
        return token;
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isActive(Instant now) {
        return !revoked && now.isBefore(expiresAt);
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("tokenHash must not be blank");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
