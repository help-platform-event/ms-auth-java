package com.maxime.help.msauth.web.dto;

/** Password complexity rule shared by signup and change-password: min 8 chars, >=2 digits, >=2 special chars. */
final class PasswordPolicy {

    static final String REGEX = "^(?=(?:.*\\d){2,})(?=(?:.*[^A-Za-z0-9]){2,}).{8,}$";
    static final String MESSAGE =
            "Password must be at least 8 characters with at least 2 digits and 2 special characters";

    private PasswordPolicy() {}
}
