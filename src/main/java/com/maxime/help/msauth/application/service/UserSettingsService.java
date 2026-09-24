package com.maxime.help.msauth.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maxime.help.msauth.application.exception.UserNotFoundException;
import com.maxime.help.msauth.domain.model.Availability;
import com.maxime.help.msauth.domain.model.NotificationSettings;
import com.maxime.help.msauth.domain.model.UserSettings;
import com.maxime.help.msauth.domain.port.out.UserRepository;
import com.maxime.help.msauth.domain.port.out.UserSettingsRepository;

/**
 * Reads and updates a user's availability and notification settings. A user who never saved any
 * settings has no row yet and gets the defaults; the row is created on the first update.
 */
@Service
public class UserSettingsService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;

    UserSettingsService(UserRepository userRepository, UserSettingsRepository userSettingsRepository) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
    }

    @Transactional(readOnly = true)
    public Availability getAvailability(UUID userId) {
        return load(userId).getAvailability();
    }

    @Transactional
    public Availability updateAvailability(UUID userId, Availability availability) {
        UserSettings settings = load(userId);
        settings.changeAvailability(availability);
        return userSettingsRepository.save(settings).getAvailability();
    }

    @Transactional(readOnly = true)
    public NotificationSettings getNotifications(UUID userId) {
        return load(userId).getNotifications();
    }

    @Transactional
    public NotificationSettings updateNotifications(UUID userId, NotificationSettings notifications) {
        UserSettings settings = load(userId);
        settings.changeNotifications(notifications);
        return userSettingsRepository.save(settings).getNotifications();
    }

    private UserSettings load(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }
        return userSettingsRepository.findByUserId(userId).orElseGet(() -> UserSettings.defaultsFor(userId));
    }
}
