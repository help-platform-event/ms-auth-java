package com.maxime.help.msauth.web.dto;

import jakarta.validation.constraints.NotNull;

/** Weekly availability, used both as request body and response. Every day is required. */
public record AvailabilityDto(
        @NotNull Boolean monday,
        @NotNull Boolean tuesday,
        @NotNull Boolean wednesday,
        @NotNull Boolean thursday,
        @NotNull Boolean friday,
        @NotNull Boolean saturday,
        @NotNull Boolean sunday) {}
