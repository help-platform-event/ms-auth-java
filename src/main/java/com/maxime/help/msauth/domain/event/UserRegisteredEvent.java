package com.maxime.help.msauth.domain.event;

import java.time.Instant;
import java.util.UUID;

/** A new account was created, whether via password signup or a first-time Google sign-in. */
public record UserRegisteredEvent(UUID eventId, Instant occurredAt, UUID userId, String email)
        implements DomainEvent {}
