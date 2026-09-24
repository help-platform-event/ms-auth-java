package com.maxime.help.msauth.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maxime.help.msauth.application.exception.UserNotFoundException;
import com.maxime.help.msauth.domain.model.Address;
import com.maxime.help.msauth.domain.model.Profile;
import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.domain.port.out.UserRepository;

/**
 * Reads and replaces a user's profile. Reads are transactional too: mapping a user touches the
 * lazy {@code Profile} association, which needs an open persistence context.
 */
@Service
public class UserProfileService {

    private final UserRepository userRepository;

    UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Profile getProfile(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new).getProfile();
    }

    /** Replaces the whole profile: fields left {@code null} are cleared. */
    @Transactional
    public Profile updateProfile(
            UUID userId,
            String firstName,
            String lastName,
            String avatarUrl,
            String phone,
            String bio,
            Address address) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Profile profile = user.getProfile();
        profile.changeName(firstName, lastName);
        profile.changeAvatarUrl(avatarUrl);
        profile.changePhone(phone);
        profile.changeBio(bio);
        profile.changeAddress(address);
        return userRepository.save(user).getProfile();
    }
}
