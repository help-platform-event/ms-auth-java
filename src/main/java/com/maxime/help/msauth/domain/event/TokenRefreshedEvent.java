package com.maxime.help.msauth.domain.event;

import java.time.Instant;
import java.util.UUID;

/** A refresh token was rotated: the presented token was revoked and a new pair was issued. */
public record TokenRefreshedEvent(UUID eventId, Instant occurredAt, UUID userId)
        implements DomainEvent {}
