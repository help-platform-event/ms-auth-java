package com.maxime.help.msauth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository over {@link UserJpaEntity}. Package-private: only the adapter uses it. */
interface SpringDataUserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByGoogleSub(String googleSub);

    boolean existsByEmail(String email);
}
