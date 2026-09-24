package com.maxime.help.msauth.domain.event;

import java.time.Instant;
import java.util.UUID;

/** A user successfully authenticated, via password or Google, and was issued a token pair. */
public record LoginSucceededEvent(UUID eventId, Instant occurredAt, UUID userId)
        implements DomainEvent {}
