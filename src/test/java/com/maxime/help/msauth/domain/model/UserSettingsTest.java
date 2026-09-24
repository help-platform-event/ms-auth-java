package com.maxime.help.msauth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain test — no Spring, no database. */
class UserSettingsTest {

    private final UUID userId = UUID.randomUUID();

    @Test
    void defaultsFor_turnsEverythingOn() {
        UserSettings settings = UserSettings.defaultsFor(userId);

        assertThat(settings.getUserId()).isEqualTo(userId);
        assertThat(settings.getAvailability()).isEqualTo(new Availability(true, true, true, true, true, true, true));
        assertThat(settings.getNotifications())
                .isEqualTo(new NotificationSettings(true, true, true, true, true, true, true));
    }

    @Test
    void defaultsFor_requiresAUserId() {
        assertThatThrownBy(() -> UserSettings.defaultsFor(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void changeAvailability_replacesIt() {
        UserSettings settings = UserSettings.defaultsFor(userId);
        Availability weekdays = new Availability(true, true, true, true, true, false, false);

        settings.changeAvailability(weekdays);

        assertThat(settings.getAvailability()).isEqualTo(weekdays);
    }

    @Test
    void changeNotifications_replacesIt() {
        UserSettings settings = UserSettings.defaultsFor(userId);
        NotificationSettings muted = new NotificationSettings(false, true, true, true, true, true, true);

        settings.changeNotifications(muted);

        assertThat(settings.getNotifications()).isEqualTo(muted);
    }

    @Test
    void changes_rejectNull() {
        UserSettings settings = UserSettings.defaultsFor(userId);

        assertThatThrownBy(() -> settings.changeAvailability(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> settings.changeNotifications(null)).isInstanceOf(NullPointerException.class);
    }
}
