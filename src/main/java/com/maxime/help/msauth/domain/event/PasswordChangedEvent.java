package com.maxime.help.msauth.domain.event;

import java.time.Instant;
import java.util.UUID;

/** A user successfully changed their password. */
public record PasswordChangedEvent(UUID eventId, Instant occurredAt, UUID userId)
        implements DomainEvent {}
