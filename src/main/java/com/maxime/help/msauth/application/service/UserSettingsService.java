package com.maxime.help.msauth.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maxime.help.msauth.application.exception.UserNotFoundException;
import com.maxime.help.msauth.domain.event.UserSettingsChangedEvent;
import com.maxime.help.msauth.domain.model.Availability;
import com.maxime.help.msauth.domain.model.NotificationSettings;
import com.maxime.help.msauth.domain.model.UserSettings;
import com.maxime.help.msauth.domain.port.out.DomainEventPublisher;
import com.maxime.help.msauth.domain.port.out.UserRepository;
import com.maxime.help.msauth.domain.port.out.UserSettingsRepository;

/**
 * Reads and updates a user's availability and notification settings. A user who never saved any
 * settings has no row yet and gets the defaults; the row is created on the first update. Every
 * update publishes a {@link UserSettingsChangedEvent} carrying the full new snapshot.
 */
@Service
public class UserSettingsService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    UserSettingsService(
            UserRepository userRepository,
            UserSettingsRepository userSettingsRepository,
            DomainEventPublisher eventPublisher,
            Clock clock) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Availability getAvailability(UUID userId) {
        return load(userId).getAvailability();
    }

    @Transactional
    public Availability updateAvailability(UUID userId, Availability availability) {
        UserSettings settings = load(userId);
        settings.changeAvailability(availability);
        return saveAndPublish(settings).getAvailability();
    }

    @Transactional(readOnly = true)
    public NotificationSettings getNotifications(UUID userId) {
        return load(userId).getNotifications();
    }

    @Transactional
    public NotificationSettings updateNotifications(UUID userId, NotificationSettings notifications) {
        UserSettings settings = load(userId);
        settings.changeNotifications(notifications);
        return saveAndPublish(settings).getNotifications();
    }

    private UserSettings saveAndPublish(UserSettings settings) {
        UserSettings saved = userSettingsRepository.save(settings);
        eventPublisher.publish(new UserSettingsChangedEvent(
                UUID.randomUUID(),
                clock.instant(),
                saved.getUserId(),
                saved.getAvailability(),
                saved.getNotifications()));
        return saved;
    }

    private UserSettings load(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }
        return userSettingsRepository.findByUserId(userId).orElseGet(() -> UserSettings.defaultsFor(userId));
    }
}
