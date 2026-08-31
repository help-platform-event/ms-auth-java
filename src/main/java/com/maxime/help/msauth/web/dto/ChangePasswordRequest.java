package com.maxime.help.msauth.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String newPassword,
        @NotBlank String confirmPassword) {

    @AssertTrue(message = "confirmPassword must match newPassword")
    public boolean isConfirmPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
