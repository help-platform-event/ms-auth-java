package com.maxime.help.msauth.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Full replacement of the caller's profile: omitted optional fields are cleared. */
public record UpdateProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String avatarUrl,
        @Pattern(regexp = "^[0-9+().\\s-]{6,20}$", message = "invalid phone number") String phone,
        @Size(max = 1000) String bio,
        @Valid AddressDto address) {}
