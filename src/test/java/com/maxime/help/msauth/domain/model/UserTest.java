package com.maxime.help.msauth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Pure domain test — no Spring, no database. */
class UserTest {

    @Test
    void register_createsLocalUserWithEmptyProfileAndUserRole() {
        User user = User.register("alice@example.com", "hash");

        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.hasPassword()).isTrue();
        assertThat(user.getGoogleSub()).isNull();
        assertThat(user.getProfile()).isNotNull();
        assertThat(user.getId()).isNull();
    }

    @Test
    void registerAdmin_createsLocalUserWithAdminRole() {
        User admin = User.registerAdmin("admin@example.com", "hash");

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.hasPassword()).isTrue();
        assertThat(admin.getProfile()).isNotNull();
    }

    @Test
    void registerWithGoogle_createsPasswordlessUser() {
        User user = User.registerWithGoogle("bob@example.com", "google-sub-123");

        assertThat(user.hasPassword()).isFalse();
        assertThat(user.getGoogleSub()).isEqualTo("google-sub-123");
    }

    @Test
    void register_rejectsBlankEmail() {
        assertThatThrownBy(() -> User.register("  ", "hash"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changePassword_rejectsBlankHash() {
        User user = User.register("alice@example.com", "hash");

        assertThatThrownBy(() -> user.changePassword(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void linkGoogleAccount_isIdempotentForSameSubButRejectsAnother() {
        User user = User.register("alice@example.com", "hash");

        user.linkGoogleAccount("sub-1");
        user.linkGoogleAccount("sub-1");
        assertThat(user.getGoogleSub()).isEqualTo("sub-1");

        assertThatThrownBy(() -> user.linkGoogleAccount("sub-2"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void disableTwoFactor_clearsSecret() {
        User user = User.register("alice@example.com", "hash");
        user.enableTwoFactor("secret");

        user.disableTwoFactor();

        assertThat(user.isTwoFactorEnabled()).isFalse();
        assertThat(user.getTwoFactorSecret()).isNull();
    }
}
