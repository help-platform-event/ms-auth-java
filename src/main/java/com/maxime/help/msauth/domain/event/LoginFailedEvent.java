package com.maxime.help.msauth.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * A password login attempt was rejected. No {@code userId}: the attempted email may not
 * correspond to any real account.
 */
public record LoginFailedEvent(UUID eventId, Instant occurredAt, String email)
        implements DomainEvent {}
