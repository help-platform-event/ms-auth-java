package com.maxime.help.msauth.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Bean Validation only — no Spring context needed. */
class SignupRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void password_acceptsExactlyTwoDigitsAndTwoSpecialCharacters() {
        SignupRequest request = new SignupRequest("alice@example.com", "ab12!!cd", "Alice", "Smith");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void password_rejectsOnlyOneDigit() {
        SignupRequest request = new SignupRequest("alice@example.com", "ab1!!!cd", "Alice", "Smith");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void password_rejectsOnlyOneSpecialCharacter() {
        SignupRequest request = new SignupRequest("alice@example.com", "ab12!3cd4", "Alice", "Smith");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void password_rejectsFewerThanEightCharacters() {
        SignupRequest request = new SignupRequest("alice@example.com", "a1!", "Alice", "Smith");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void email_rejectsInvalidFormat() {
        SignupRequest request = new SignupRequest("not-an-email", "ab12!!cd", "Alice", "Smith");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}
