package com.maxime.help.msauth.web.dto;

import jakarta.validation.constraints.NotNull;

/** Notification settings, used both as request body and response. Every flag is required. */
public record NotificationSettingsDto(
        @NotNull Boolean enabled,
        @NotNull Boolean eventActivity,
        @NotNull Boolean eventMessages,
        @NotNull Boolean documents,
        @NotNull Boolean deadlines,
        @NotNull Boolean nearbyEvents,
        @NotNull Boolean judgments) {}
