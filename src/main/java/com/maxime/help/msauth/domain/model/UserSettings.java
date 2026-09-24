package com.maxime.help.msauth.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate holding a user's non-authentication settings: availability and notification settings.
 * Kept apart from {@link User} so that authentication flows never load it. Identified by the
 * owning user's id, which must already exist.
 */
public class UserSettings {

    private UUID userId;
    private Availability availability;
    private NotificationSettings notifications;

    private UserSettings() {
    }

    /** Default settings for a user who has never changed them. */
    public static UserSettings defaultsFor(UUID userId) {
        UserSettings settings = new UserSettings();
        settings.userId = Objects.requireNonNull(userId, "userId must not be null");
        settings.availability = Availability.allDays();
        settings.notifications = NotificationSettings.defaults();
        return settings;
    }

    /** Rebuilds settings from persisted state. Infrastructure use only. */
    public static UserSettings reconstitute(
            UUID userId, Availability availability, NotificationSettings notifications) {
        UserSettings settings = new UserSettings();
        settings.userId = userId;
        settings.availability = availability;
        settings.notifications = notifications;
        return settings;
    }

    public void changeAvailability(Availability availability) {
        this.availability = Objects.requireNonNull(availability, "availability must not be null");
    }

    public void changeNotifications(NotificationSettings notifications) {
        this.notifications = Objects.requireNonNull(notifications, "notifications must not be null");
    }

    public UUID getUserId() {
        return userId;
    }

    public Availability getAvailability() {
        return availability;
    }

    public NotificationSettings getNotifications() {
        return notifications;
    }
}
