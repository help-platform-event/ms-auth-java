package com.maxime.help.msauth.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maxime.help.msauth.domain.event.UserRegisteredEvent;
import com.maxime.help.msauth.domain.model.PasswordPolicy;
import com.maxime.help.msauth.domain.model.Role;
import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.domain.port.out.DomainEventPublisher;
import com.maxime.help.msauth.domain.port.out.PasswordHasher;
import com.maxime.help.msauth.domain.port.out.UserRepository;

/**
 * Guarantees a usable administrator account exists, so that a freshly wiped database is never
 * left without one. Idempotent: does nothing once any ADMIN exists.
 */
@Service
public class AdminProvisioningService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    AdminProvisioningService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            DomainEventPublisher eventPublisher,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Creates the admin unless one already exists. Refuses to promote an existing non-admin
     * account with the same email: whoever registered it knows its password, so promoting it would
     * hand them admin rights.
     */
    @Transactional
    public AdminProvisioningResult ensureAdminExists(String email, String rawPassword) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return AdminProvisioningResult.ALREADY_PRESENT;
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "Admin email is already registered as a non-admin account; refusing to promote it");
        }
        if (!PasswordPolicy.isSatisfiedBy(rawPassword)) {
            throw new IllegalArgumentException("Admin password: " + PasswordPolicy.MESSAGE);
        }
        User saved = userRepository.save(User.registerAdmin(email, passwordHasher.hash(rawPassword)));
        eventPublisher.publish(
                new UserRegisteredEvent(UUID.randomUUID(), clock.instant(), saved.getId(), saved.getEmail()));
        return AdminProvisioningResult.CREATED;
    }
}
