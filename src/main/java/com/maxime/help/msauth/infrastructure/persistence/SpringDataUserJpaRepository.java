package com.maxime.help.msauth.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository over {@link UserJpaEntity}. Package-private: only the adapter uses it. */
interface SpringDataUserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByGoogleSub(String googleSub);

    boolean existsByEmail(String email);

    /** Fetches the profiles in the same query, so mapping a batch of users doesn't issue N+1 selects. */
    @EntityGraph(attributePaths = "profile")
    List<UserJpaEntity> findAllByIdIn(Collection<UUID> ids);

    /** Same as {@link #findAll()}, with the profiles fetched in the same query. */
    @EntityGraph(attributePaths = "profile")
    List<UserJpaEntity> findAllBy();
}
