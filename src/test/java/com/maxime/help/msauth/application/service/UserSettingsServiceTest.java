package com.maxime.help.msauth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maxime.help.msauth.application.exception.UserNotFoundException;
import com.maxime.help.msauth.domain.model.Availability;
import com.maxime.help.msauth.domain.model.NotificationSettings;
import com.maxime.help.msauth.domain.model.UserSettings;
import com.maxime.help.msauth.domain.port.out.UserRepository;
import com.maxime.help.msauth.domain.port.out.UserSettingsRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pure application-service test — every port is mocked, no Spring, no database. */
@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Availability WEEKDAYS = new Availability(true, true, true, true, true, false, false);

    @Mock private UserRepository userRepository;
    @Mock private UserSettingsRepository userSettingsRepository;

    @InjectMocks private UserSettingsService service;

    @Test
    void getAvailability_returnsDefaultsWhenNothingWasSavedYet() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(service.getAvailability(USER_ID)).isEqualTo(Availability.allDays());
        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    void getNotifications_returnsTheSavedSettings() {
        NotificationSettings muted = new NotificationSettings(false, true, true, true, true, true, true);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userSettingsRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(UserSettings.reconstitute(USER_ID, Availability.allDays(), muted)));

        assertThat(service.getNotifications(USER_ID)).isEqualTo(muted);
    }

    @Test
    void updateAvailability_createsTheRowOnFirstUpdateAndKeepsDefaultNotifications() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        Availability result = service.updateAvailability(USER_ID, WEEKDAYS);

        ArgumentCaptor<UserSettings> saved = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(saved.capture());
        assertThat(result).isEqualTo(WEEKDAYS);
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getNotifications()).isEqualTo(NotificationSettings.defaults());
    }

    @Test
    void updateNotifications_keepsTheExistingAvailability() {
        NotificationSettings muted = new NotificationSettings(false, false, false, false, false, false, false);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userSettingsRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(UserSettings.reconstitute(USER_ID, WEEKDAYS, NotificationSettings.defaults())));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.updateNotifications(USER_ID, muted)).isEqualTo(muted);

        ArgumentCaptor<UserSettings> saved = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(saved.capture());
        assertThat(saved.getValue().getAvailability()).isEqualTo(WEEKDAYS);
    }

    @Test
    void everyOperation_throwsWhenUserIsUnknown() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getAvailability(USER_ID)).isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> service.updateNotifications(USER_ID, NotificationSettings.defaults()))
                .isInstanceOf(UserNotFoundException.class);
        verify(userSettingsRepository, never()).save(any());
    }
}
