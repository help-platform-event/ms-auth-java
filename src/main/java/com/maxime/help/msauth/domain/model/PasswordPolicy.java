package com.maxime.help.msauth.domain.model;

import java.util.regex.Pattern;

/**
 * Password complexity rule: min 8 chars, at least 2 digits and 2 special characters. The constants
 * are compile-time constants so web DTOs can use them in {@code @Pattern}; code outside the web
 * layer (e.g. the admin seed) checks raw passwords with {@link #isSatisfiedBy}.
 */
public final class PasswordPolicy {

    public static final String REGEX = "^(?=(?:.*\\d){2,})(?=(?:.*[^A-Za-z0-9]){2,}).{8,}$";
    public static final String MESSAGE =
            "Password must be at least 8 characters with at least 2 digits and 2 special characters";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private PasswordPolicy() {}

    public static boolean isSatisfiedBy(String rawPassword) {
        return rawPassword != null && PATTERN.matcher(rawPassword).matches();
    }
}
