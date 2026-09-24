package com.maxime.help.msauth.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository over {@link UserSettingsJpaEntity}. Package-private: only the adapter uses it. */
interface SpringDataUserSettingsJpaRepository extends JpaRepository<UserSettingsJpaEntity, UUID> {}
