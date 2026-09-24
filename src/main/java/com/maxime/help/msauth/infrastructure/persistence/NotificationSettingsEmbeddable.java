package com.maxime.help.msauth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA embeddable mirroring the domain {@code NotificationSettings} value object. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class NotificationSettingsEmbeddable {

    @Column(name = "notifications_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "notify_event_activity", nullable = false)
    private boolean eventActivity;

    @Column(name = "notify_event_messages", nullable = false)
    private boolean eventMessages;

    @Column(name = "notify_documents", nullable = false)
    private boolean documents;

    @Column(name = "notify_deadlines", nullable = false)
    private boolean deadlines;

    @Column(name = "notify_nearby_events", nullable = false)
    private boolean nearbyEvents;

    @Column(name = "notify_judgments", nullable = false)
    private boolean judgments;
}
