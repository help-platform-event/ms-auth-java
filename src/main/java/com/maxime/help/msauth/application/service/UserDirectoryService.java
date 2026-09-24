package com.maxime.help.msauth.application.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maxime.help.msauth.application.exception.UserNotFoundException;
import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.domain.port.out.UserRepository;

/**
 * User lookups for other services (the Gateway): by id, by a batch of ids, or all users.
 * Transactional because mapping a user touches the lazy {@code Profile} association.
 */
@Service
@Transactional(readOnly = true)
public class UserDirectoryService {

    private final UserRepository userRepository;

    UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    /** Users whose id is in {@code userIds}; unknown ids are silently skipped. */
    public List<User> findByIds(Collection<UUID> userIds) {
        return userRepository.findAllByIds(userIds);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
