package com.maxime.help.msauth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Sha256TokenHasherAdapterTest {

    private final Sha256TokenHasherAdapter adapter = new Sha256TokenHasherAdapter();

    @Test
    void hash_isDeterministicForTheSameInput() {
        assertThat(adapter.hash("some-opaque-token")).isEqualTo(adapter.hash("some-opaque-token"));
    }

    @Test
    void hash_producesDifferentOutputForDifferentInput() {
        assertThat(adapter.hash("token-a")).isNotEqualTo(adapter.hash("token-b"));
    }
}
