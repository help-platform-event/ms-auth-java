package com.maxime.help.msauth.domain.event;

import com.maxime.help.msauth.domain.model.Availability;
import com.maxime.help.msauth.domain.model.NotificationSettings;
import java.time.Instant;
import java.util.UUID;

/**
 * A user's settings changed. Carries the full current snapshot (not a delta), so a consumer can
 * keep its own copy from the latest event alone — and a log-compacted topic, which keeps only the
 * last message per key, still holds every user's current settings.
 */
public record UserSettingsChangedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        Availability availability,
        NotificationSettings notifications)
        implements DomainEvent {}
