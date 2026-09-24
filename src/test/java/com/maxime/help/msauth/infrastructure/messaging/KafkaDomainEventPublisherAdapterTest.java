package com.maxime.help.msauth.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.maxime.help.msauth.domain.event.DomainEvent;
import com.maxime.help.msauth.domain.event.LoggedOutEvent;
import com.maxime.help.msauth.domain.event.LoginFailedEvent;
import com.maxime.help.msauth.domain.event.LoginSucceededEvent;
import com.maxime.help.msauth.domain.event.PasswordChangedEvent;
import com.maxime.help.msauth.domain.event.TokenRefreshedEvent;
import com.maxime.help.msauth.domain.event.UserRegisteredEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/** No Spring context: transactional behavior is exercised directly via TransactionSynchronizationManager. */
@ExtendWith(MockitoExtension.class)
class KafkaDomainEventPublisherAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaDomainEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaDomainEventPublisherAdapter(kafkaTemplate);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // --- topic/key resolution, no active transaction: sends immediately ---

    @Test
    void publish_sendsUserRegisteredEventToItsTopic() {
        UserRegisteredEvent event = new UserRegisteredEvent(UUID.randomUUID(), NOW, USER_ID, "alice@example.com");

        adapter.publish(event);

        verify(kafkaTemplate).send("auth.user.registered", USER_ID.toString(), event);
    }

    @Test
    void publish_sendsPasswordChangedEventToItsTopic() {
        PasswordChangedEvent event = new PasswordChangedEvent(UUID.randomUUID(), NOW, USER_ID);

        adapter.publish(event);

        verify(kafkaTemplate).send("auth.password.changed", USER_ID.toString(), event);
    }

    @Test
    void publish_sendsLoginFailedEventKeyedByEmail() {
        LoginFailedEvent event = new LoginFailedEvent(UUID.randomUUID(), NOW, "alice@example.com");

        adapter.publish(event);

        verify(kafkaTemplate).send("auth.login.failed", "alice@example.com", event);
    }

    @Test
    void publish_sendsLoginSucceededEventToItsTopic() {
        LoginSucceededEvent event = new LoginSucceededEvent(UUID.randomUUID(), NOW, USER_ID);

        adapter.publish(event);

        verify(kafkaTemplate).send("auth.login.succeeded", USER_ID.toString(), event);
    }

    @Test
    void publish_sendsTokenRefreshedEventToItsTopic() {
        TokenRefreshedEvent event = new TokenRefreshedEvent(UUID.randomUUID(), NOW, USER_ID);

        adapter.publish(event);

        verify(kafkaTemplate).send("auth.token.refreshed", USER_ID.toString(), event);
    }

    @Test
    void publish_sendsLoggedOutEventToItsTopic() {
        LoggedOutEvent event = new LoggedOutEvent(UUID.randomUUID(), NOW, USER_ID);

        adapter.publish(event);

        verify(kafkaTemplate).send("auth.logout", USER_ID.toString(), event);
    }

    // --- transaction-synchronization behavior ---

    @Test
    void publish_deferSendUntilTheActiveTransactionCommits() {
        DomainEvent event = new PasswordChangedEvent(UUID.randomUUID(), NOW, USER_ID);
        TransactionSynchronizationManager.initSynchronization();

        adapter.publish(event);
        verify(kafkaTemplate, never()).send(any(), any(), any());

        TransactionSynchronizationUtils.triggerAfterCommit();

        verify(kafkaTemplate).send("auth.password.changed", USER_ID.toString(), event);
    }

    @Test
    void publish_neverSendsWhenTheActiveTransactionRollsBackInstead() {
        DomainEvent event = new PasswordChangedEvent(UUID.randomUUID(), NOW, USER_ID);
        TransactionSynchronizationManager.initSynchronization();

        adapter.publish(event);
        TransactionSynchronizationManager.clearSynchronization();

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
