package com.maxime.help.msauth.infrastructure.persistence;

import com.maxime.help.msauth.domain.model.UserSettings;
import com.maxime.help.msauth.domain.port.out.UserSettingsRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapts Spring Data JPA to the {@link UserSettingsRepository} port. */
@Component
class UserSettingsRepositoryAdapter implements UserSettingsRepository {

    private final SpringDataUserSettingsJpaRepository jpa;
    private final UserSettingsPersistenceMapper mapper;

    UserSettingsRepositoryAdapter(SpringDataUserSettingsJpaRepository jpa, UserSettingsPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public UserSettings save(UserSettings settings) {
        UserSettingsJpaEntity entity = jpa.findById(settings.getUserId())
                .map(existing -> {
                    mapper.updateEntity(existing, settings);
                    return existing;
                })
                .orElseGet(() -> mapper.toEntity(settings));
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<UserSettings> findByUserId(UUID userId) {
        return jpa.findById(userId).map(mapper::toDomain);
    }
}
