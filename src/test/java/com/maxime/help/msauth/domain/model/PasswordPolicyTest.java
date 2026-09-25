package com.maxime.help.msauth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Pure domain test — no Spring, no database. */
class PasswordPolicyTest {

    @Test
    void acceptsAPasswordWithTwoDigitsAndTwoSpecialCharacters() {
        assertThat(PasswordPolicy.isSatisfiedBy("Admin123!!")).isTrue();
    }

    @Test
    void rejectsWeakOrMissingPasswords() {
        assertThat(PasswordPolicy.isSatisfiedBy("short1!")).isFalse();
        assertThat(PasswordPolicy.isSatisfiedBy("NoDigits!!")).isFalse();
        assertThat(PasswordPolicy.isSatisfiedBy("NoSpecial12")).isFalse();
        assertThat(PasswordPolicy.isSatisfiedBy(null)).isFalse();
    }
}
