package com.maxime.help.msauth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** No Spring context needed — exercises the real Argon2 implementation directly. */
class Argon2PasswordHasherAdapterTest {

    private final Argon2PasswordHasherAdapter adapter = new Argon2PasswordHasherAdapter();

    @Test
    void hash_producesDifferentHashesForTheSamePasswordButBothVerify() {
        String first = adapter.hash("Passw0rd!!");
        String second = adapter.hash("Passw0rd!!");

        assertThat(first).isNotEqualTo(second);
        assertThat(adapter.matches("Passw0rd!!", first)).isTrue();
        assertThat(adapter.matches("Passw0rd!!", second)).isTrue();
    }

    @Test
    void matches_returnsFalseForWrongPassword() {
        String hash = adapter.hash("Passw0rd!!");

        assertThat(adapter.matches("wrong-password", hash)).isFalse();
    }

    @Test
    void argon2PasswordEncoder_isConfiguredWithExpectedParameters() {
        String hash = adapter.hash("Passw0rd!!");

        assertThat(hash).startsWith("$argon2id$v=19$m=65536,t=3,p=4$");
    }
}
