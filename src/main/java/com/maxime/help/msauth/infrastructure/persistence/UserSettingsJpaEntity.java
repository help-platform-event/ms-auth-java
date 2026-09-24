package com.maxime.help.msauth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA mapping for {@code user_settings}. The primary key is the owning user's id, assigned by the
 * caller rather than generated. There is deliberately no JPA association to {@link UserJpaEntity}:
 * the foreign key lives in the schema only, so loading settings never drags the user graph along.
 */
@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
public class UserSettingsJpaEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "binary(16)", nullable = false, updatable = false)
    private UUID userId;

    @Embedded
    private AvailabilityEmbeddable availability;

    @Embedded
    private NotificationSettingsEmbeddable notifications;
}
