package com.maxime.help.msauth.infrastructure.persistence;

import com.maxime.help.msauth.domain.model.RefreshToken;
import com.maxime.help.msauth.domain.port.out.RefreshTokenRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adapts Spring Data JPA to the {@link RefreshTokenRepository} port. */
@Component
class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final SpringDataRefreshTokenJpaRepository jpa;
    private final RefreshTokenPersistenceMapper mapper;

    RefreshTokenRepositoryAdapter(
            SpringDataRefreshTokenJpaRepository jpa, RefreshTokenPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity;
        if (token.getId() == null) {
            entity = mapper.toEntity(token);
        } else {
            entity = jpa.findById(token.getId())
                    .orElseThrow(() ->
                            new IllegalStateException("Refresh token not found: " + token.getId()));
            entity.setRevoked(token.isRevoked());
        }
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByToken(tokenHash).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpa.deleteByUserId(userId);
    }
}
