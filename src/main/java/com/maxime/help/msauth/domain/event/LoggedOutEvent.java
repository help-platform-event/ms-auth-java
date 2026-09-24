package com.maxime.help.msauth.domain.event;

import java.time.Instant;
import java.util.UUID;

/** A live refresh token was found and revoked for the calling user. */
public record LoggedOutEvent(UUID eventId, Instant occurredAt, UUID userId)
        implements DomainEvent {}
