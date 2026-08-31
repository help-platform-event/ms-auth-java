package com.maxime.help.msauth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root for authentication. Pure domain object: no framework annotations, no public
 * setters. State changes go through the business methods, which validate before they mutate.
 *
 * <p>{@code id}, {@code createdAt} and {@code updatedAt} are assigned by the persistence adapter
 * and are {@code null} on an instance that has never been saved.
 */
public class User {

    private UUID id;
    private String email;
    private String passwordHash;
    private Role role;
    private String googleSub;
    private boolean twoFactorEnabled;
    private String twoFactorSecret;
    private Profile profile;
    private Instant createdAt;
    private Instant updatedAt;

    private User() {
    }

    /** Registers a new local (email + password) account. */
    public static User register(String email, String passwordHash) {
        User user = new User();
        user.email = requireText(email, "email");
        user.passwordHash = requireHash(passwordHash);
        user.role = Role.USER;
        user.profile = Profile.empty();
        return user;
    }

    /** Registers a new account that authenticates only through Google (no local password). */
    public static User registerWithGoogle(String email, String googleSub) {
        User user = new User();
        user.email = requireText(email, "email");
        user.googleSub = requireText(googleSub, "googleSub");
        user.role = Role.USER;
        user.profile = Profile.empty();
        return user;
    }

    /** Rebuilds a user from persisted state. Infrastructure use only. */
    public static User reconstitute(
            UUID id,
            String email,
            String passwordHash,
            Role role,
            String googleSub,
            boolean twoFactorEnabled,
            String twoFactorSecret,
            Profile profile,
            Instant createdAt,
            Instant updatedAt) {
        User user = new User();
        user.id = id;
        user.email = email;
        user.passwordHash = passwordHash;
        user.role = role;
        user.googleSub = googleSub;
        user.twoFactorEnabled = twoFactorEnabled;
        user.twoFactorSecret = twoFactorSecret;
        user.profile = profile != null ? profile : Profile.empty();
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        return user;
    }

    /** Sets a new password hash. Hashing is the caller's job — never done in the domain. */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = requireHash(newPasswordHash);
    }

    /** Links this account to a Google identity. Fails if already linked to a different one. */
    public void linkGoogleAccount(String googleSub) {
        String sub = requireText(googleSub, "googleSub");
        if (this.googleSub != null && !this.googleSub.equals(sub)) {
            throw new IllegalStateException("Account is already linked to a different Google identity");
        }
        this.googleSub = sub;
    }

    public void enableTwoFactor(String secret) {
        this.twoFactorSecret = requireText(secret, "secret");
        this.twoFactorEnabled = true;
    }

    public void disableTwoFactor() {
        this.twoFactorEnabled = false;
        this.twoFactorSecret = null;
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String requireHash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public Profile getProfile() {
        return profile;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
