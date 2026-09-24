package com.maxime.help.msauth.domain.port.out;

import com.maxime.help.msauth.domain.model.UserSettings;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for user-settings persistence. Implemented in {@code infrastructure.persistence}. */
public interface UserSettingsRepository {

    /** Inserts or updates the settings of {@link UserSettings#getUserId()}. */
    UserSettings save(UserSettings settings);

    Optional<UserSettings> findByUserId(UUID userId);
}
