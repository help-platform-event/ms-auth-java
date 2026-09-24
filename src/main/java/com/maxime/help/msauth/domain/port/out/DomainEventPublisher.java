package com.maxime.help.msauth.domain.port.out;

import com.maxime.help.msauth.domain.event.DomainEvent;

/**
 * Outbound port for publishing domain events. Implemented in {@code infrastructure.messaging} by
 * a Kafka adapter, which is responsible for deciding when the send actually happens (e.g. only
 * after the enclosing transaction commits) — callers just report the fact.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
