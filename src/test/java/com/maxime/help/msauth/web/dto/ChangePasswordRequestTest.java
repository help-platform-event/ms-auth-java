package com.maxime.help.msauth.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChangePasswordRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void isConfirmPasswordMatching_passesWhenNewAndConfirmMatch() {
        ChangePasswordRequest request =
                new ChangePasswordRequest("current-password", "ab12!!cd", "ab12!!cd");

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void isConfirmPasswordMatching_failsWhenNewAndConfirmDiffer() {
        ChangePasswordRequest request =
                new ChangePasswordRequest("current-password", "ab12!!cd", "different!!12");

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> v.getMessage().equals("confirmPassword must match newPassword"));
    }
}
