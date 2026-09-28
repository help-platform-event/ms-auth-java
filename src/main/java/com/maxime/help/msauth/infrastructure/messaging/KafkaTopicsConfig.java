package com.maxime.help.msauth.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Declares the topics this service publishes to, instead of relying on broker auto-creation
 * (1 partition, default retention). Spring's {@link KafkaAdmin} creates them at startup, and adds
 * partitions to an existing topic that has fewer — it never removes any.
 *
 * <p>3 partitions: events are keyed by user id, so each user's events stay ordered on one
 * partition while up to 3 consumer instances of a group can share the load.
 */
@Configuration
class KafkaTopicsConfig {

    private static final int PARTITIONS = 3;

    /**
     * Kept forever: it is the only source of a user's email for consumers that build their own
     * projection of users (e.g. the notification service), including one starting from scratch.
     */
    @Bean
    NewTopic userRegisteredTopic() {
        return TopicBuilder.name(AuthTopics.USER_REGISTERED)
                .partitions(PARTITIONS)
                .config(TopicConfig.RETENTION_MS_CONFIG, "-1")
                .build();
    }

    /**
     * Log-compacted: Kafka eventually keeps only the latest message per key (user id), so the
     * topic always holds every user's current settings snapshot, however old.
     */
    @Bean
    NewTopic userSettingsChangedTopic() {
        return TopicBuilder.name(AuthTopics.USER_SETTINGS_CHANGED)
                .partitions(PARTITIONS)
                .compact()
                .build();
    }

    @Bean
    NewTopic passwordChangedTopic() {
        return TopicBuilder.name(AuthTopics.PASSWORD_CHANGED).partitions(PARTITIONS).build();
    }

    @Bean
    NewTopic loginFailedTopic() {
        return TopicBuilder.name(AuthTopics.LOGIN_FAILED).partitions(PARTITIONS).build();
    }

    @Bean
    NewTopic loginSucceededTopic() {
        return TopicBuilder.name(AuthTopics.LOGIN_SUCCEEDED).partitions(PARTITIONS).build();
    }

    @Bean
    NewTopic tokenRefreshedTopic() {
        return TopicBuilder.name(AuthTopics.TOKEN_REFRESHED).partitions(PARTITIONS).build();
    }

    @Bean
    NewTopic loggedOutTopic() {
        return TopicBuilder.name(AuthTopics.LOGGED_OUT).partitions(PARTITIONS).build();
    }
}
