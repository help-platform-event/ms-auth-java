package com.maxime.help.msauth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository over {@link RefreshTokenJpaEntity}. Package-private: only the adapter uses it. */
interface SpringDataRefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByToken(String token);

    void deleteByUserId(UUID userId);
}
