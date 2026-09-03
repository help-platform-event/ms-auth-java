package com.maxime.help.msauth.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * A fact published about something that already happened in the auth domain. Pure domain type:
 * no framework annotations. Sealed so that any new adapter dispatching on event type (e.g. to
 * resolve a Kafka topic) gets a compiler-enforced exhaustiveness check.
 */
public sealed interface DomainEvent
        permits UserRegisteredEvent,
                PasswordChangedEvent,
                LoginFailedEvent,
                LoginSucceededEvent,
                TokenRefreshedEvent,
                LoggedOutEvent {

    UUID eventId();

    Instant occurredAt();
}
