package com.maxime.help.msauth.infrastructure.persistence;

import com.maxime.help.msauth.domain.model.RefreshToken;
import org.springframework.stereotype.Component;

/** Translates between the {@link RefreshToken} aggregate and its JPA representation. */
@Component
class RefreshTokenPersistenceMapper {

    private final SpringDataUserJpaRepository userJpaRepository;

    RefreshTokenPersistenceMapper(SpringDataUserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    RefreshTokenJpaEntity toEntity(RefreshToken token) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        // Reference proxy — no extra SELECT just to set the FK.
        entity.setUser(userJpaRepository.getReferenceById(token.getUserId()));
        entity.setToken(token.getTokenHash());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setRevoked(token.isRevoked());
        return entity;
    }

    RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.reconstitute(
                entity.getId(),
                entity.getUser().getId(),
                entity.getToken(),
                entity.getExpiresAt(),
                entity.isRevoked(),
                entity.getCreatedAt());
    }
}
