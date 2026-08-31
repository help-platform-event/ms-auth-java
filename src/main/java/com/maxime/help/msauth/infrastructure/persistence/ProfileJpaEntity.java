package com.maxime.help.msauth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA mapping for {@code profiles}. Shares the primary key of {@link UserJpaEntity} via {@link MapsId}. */
@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class ProfileJpaEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "binary(16)")
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_profiles_user"))
    private UserJpaEntity user;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(length = 30)
    private String phone;

    @Column(length = 1000)
    private String bio;

    @Embedded
    private AddressEmbeddable address;
}
