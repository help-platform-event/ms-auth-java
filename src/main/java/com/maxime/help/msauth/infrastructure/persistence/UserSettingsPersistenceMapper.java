package com.maxime.help.msauth.infrastructure.persistence;

import com.maxime.help.msauth.domain.model.Availability;
import com.maxime.help.msauth.domain.model.NotificationSettings;
import com.maxime.help.msauth.domain.model.UserSettings;
import org.springframework.stereotype.Component;

/** Translates between the {@link UserSettings} aggregate and its JPA representation. */
@Component
class UserSettingsPersistenceMapper {

    UserSettingsJpaEntity toEntity(UserSettings settings) {
        UserSettingsJpaEntity entity = new UserSettingsJpaEntity();
        entity.setUserId(settings.getUserId());
        updateEntity(entity, settings);
        return entity;
    }

    /** Copies the mutable state of {@code settings} onto an already-managed entity (update path). */
    void updateEntity(UserSettingsJpaEntity entity, UserSettings settings) {
        entity.setAvailability(toEmbeddable(settings.getAvailability()));
        entity.setNotifications(toEmbeddable(settings.getNotifications()));
    }

    UserSettings toDomain(UserSettingsJpaEntity entity) {
        AvailabilityEmbeddable a = entity.getAvailability();
        NotificationSettingsEmbeddable n = entity.getNotifications();
        return UserSettings.reconstitute(
                entity.getUserId(),
                new Availability(
                        a.isMonday(),
                        a.isTuesday(),
                        a.isWednesday(),
                        a.isThursday(),
                        a.isFriday(),
                        a.isSaturday(),
                        a.isSunday()),
                new NotificationSettings(
                        n.isEnabled(),
                        n.isEventActivity(),
                        n.isEventMessages(),
                        n.isDocuments(),
                        n.isDeadlines(),
                        n.isNearbyEvents(),
                        n.isJudgments()));
    }

    private AvailabilityEmbeddable toEmbeddable(Availability availability) {
        AvailabilityEmbeddable entity = new AvailabilityEmbeddable();
        entity.setMonday(availability.monday());
        entity.setTuesday(availability.tuesday());
        entity.setWednesday(availability.wednesday());
        entity.setThursday(availability.thursday());
        entity.setFriday(availability.friday());
        entity.setSaturday(availability.saturday());
        entity.setSunday(availability.sunday());
        return entity;
    }

    private NotificationSettingsEmbeddable toEmbeddable(NotificationSettings notifications) {
        NotificationSettingsEmbeddable entity = new NotificationSettingsEmbeddable();
        entity.setEnabled(notifications.enabled());
        entity.setEventActivity(notifications.eventActivity());
        entity.setEventMessages(notifications.eventMessages());
        entity.setDocuments(notifications.documents());
        entity.setDeadlines(notifications.deadlines());
        entity.setNearbyEvents(notifications.nearbyEvents());
        entity.setJudgments(notifications.judgments());
        return entity;
    }
}
