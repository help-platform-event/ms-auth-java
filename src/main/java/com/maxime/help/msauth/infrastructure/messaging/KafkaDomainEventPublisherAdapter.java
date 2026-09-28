package com.maxime.help.msauth.infrastructure.messaging;

import com.maxime.help.msauth.domain.event.DomainEvent;
import com.maxime.help.msauth.domain.event.PasswordChangedEvent;
import com.maxime.help.msauth.domain.event.UserRegisteredEvent;
import com.maxime.help.msauth.domain.event.UserSettingsChangedEvent;
import com.maxime.help.msauth.domain.port.out.DomainEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sends {@link DomainEvent}s to Kafka, one topic per event type. If a Spring transaction is
 * active when {@link #publish} is called, the send is deferred until that transaction commits
 * (via {@link TransactionSynchronization#afterCommit()}), so a rolled-back write never produces an
 * event describing it. With no active transaction, the send happens immediately.
 */
@Component
class KafkaDomainEventPublisherAdapter implements DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    KafkaDomainEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            send(event);
                        }
                    });
        } else {
            send(event);
        }
    }

    private void send(DomainEvent event) {
        kafkaTemplate.send(topicFor(event), keyFor(event), event);
    }

    private static String topicFor(DomainEvent event) {
        return switch (event) {
            case UserRegisteredEvent e -> AuthTopics.USER_REGISTERED;
            case PasswordChangedEvent e -> AuthTopics.PASSWORD_CHANGED;
            case UserSettingsChangedEvent e -> AuthTopics.USER_SETTINGS_CHANGED;
        };
    }

    private static String keyFor(DomainEvent event) {
        return switch (event) {
            case UserRegisteredEvent e -> e.userId().toString();
            case PasswordChangedEvent e -> e.userId().toString();
            case UserSettingsChangedEvent e -> e.userId().toString();
        };
    }
}
