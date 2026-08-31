package com.maxime.help.msauth.infrastructure.persistence;

import com.maxime.help.msauth.domain.model.Address;
import com.maxime.help.msauth.domain.model.Profile;
import com.maxime.help.msauth.domain.model.User;
import org.springframework.stereotype.Component;

/** Translates between the {@link User} aggregate and its JPA representation. */
@Component
class UserPersistenceMapper {

    /** Builds a brand-new managed graph for a user that has never been persisted. */
    UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPasswordHash());
        entity.setRole(user.getRole());
        entity.setGoogleSub(user.getGoogleSub());
        entity.setTwoFactorEnabled(user.isTwoFactorEnabled());
        entity.setTwoFactorSecret(user.getTwoFactorSecret());
        entity.setProfile(newProfileEntity(user.getProfile()));
        return entity;
    }

    /** Copies the mutable state of {@code user} onto an already-managed entity (update path). */
    void updateEntity(UserJpaEntity entity, User user) {
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPasswordHash());
        entity.setRole(user.getRole());
        entity.setGoogleSub(user.getGoogleSub());
        entity.setTwoFactorEnabled(user.isTwoFactorEnabled());
        entity.setTwoFactorSecret(user.getTwoFactorSecret());
        applyProfile(entity.getProfile(), user.getProfile());
    }

    User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getRole(),
                entity.getGoogleSub(),
                entity.isTwoFactorEnabled(),
                entity.getTwoFactorSecret(),
                toProfileDomain(entity.getProfile()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private ProfileJpaEntity newProfileEntity(Profile profile) {
        ProfileJpaEntity entity = new ProfileJpaEntity();
        applyProfile(entity, profile);
        return entity;
    }

    private void applyProfile(ProfileJpaEntity entity, Profile profile) {
        if (profile == null) {
            return;
        }
        entity.setFirstName(profile.getFirstName());
        entity.setLastName(profile.getLastName());
        entity.setAvatarUrl(profile.getAvatarUrl());
        entity.setPhone(profile.getPhone());
        entity.setBio(profile.getBio());
        entity.setAddress(toAddressEmbeddable(profile.getAddress()));
    }

    private Profile toProfileDomain(ProfileJpaEntity entity) {
        if (entity == null) {
            return Profile.empty();
        }
        return Profile.reconstitute(
                entity.getFirstName(),
                entity.getLastName(),
                entity.getAvatarUrl(),
                entity.getPhone(),
                entity.getBio(),
                toAddressDomain(entity.getAddress()));
    }

    private AddressEmbeddable toAddressEmbeddable(Address address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        AddressEmbeddable entity = new AddressEmbeddable();
        entity.setStreetNumber(address.streetNumber());
        entity.setStreetName(address.streetName());
        entity.setAddressLine2(address.addressLine2());
        entity.setCity(address.city());
        entity.setPostalCode(address.postalCode());
        entity.setCountry(address.country());
        entity.setLatitude(address.latitude());
        entity.setLongitude(address.longitude());
        return entity;
    }

    private Address toAddressDomain(AddressEmbeddable entity) {
        if (entity == null) {
            return null;
        }
        Address address = new Address(
                entity.getStreetNumber(),
                entity.getStreetName(),
                entity.getAddressLine2(),
                entity.getCity(),
                entity.getPostalCode(),
                entity.getCountry(),
                entity.getLatitude(),
                entity.getLongitude());
        return address.isEmpty() ? null : address;
    }
}
