package com.maxime.help.msauth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.maxime.help.msauth.domain.event.UserRegisteredEvent;
import com.maxime.help.msauth.domain.model.Role;
import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.domain.port.out.DomainEventPublisher;
import com.maxime.help.msauth.domain.port.out.PasswordHasher;
import com.maxime.help.msauth.domain.port.out.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pure application-service test — every port is mocked, no Spring, no database. */
@ExtendWith(MockitoExtension.class)
class AdminProvisioningServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "Admin123!!";

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private DomainEventPublisher eventPublisher;

    private AdminProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new AdminProvisioningService(
                userRepository, passwordHasher, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void doesNothingWhenAnAdminAlreadyExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        assertThat(service.ensureAdminExists(EMAIL, PASSWORD)).isEqualTo(AdminProvisioningResult.ALREADY_PRESENT);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordHasher, eventPublisher);
    }

    @Test
    void refusesToPromoteAnExistingNonAdminAccount() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.ensureAdminExists(EMAIL, PASSWORD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refusing to promote");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void rejectsAWeakPassword() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

        assertThatThrownBy(() -> service.ensureAdminExists(EMAIL, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createsTheAdminWithAHashedPasswordAndPublishesUserRegistered() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordHasher.hash(PASSWORD)).thenReturn("argon2-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.reconstitute(id, u.getEmail(), u.getPasswordHash(), u.getRole(), null, false, null,
                    u.getProfile(), NOW, NOW);
        });

        assertThat(service.ensureAdminExists(EMAIL, PASSWORD)).isEqualTo(AdminProvisioningResult.CREATED);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("argon2-hash");
        ArgumentCaptor<UserRegisteredEvent> event = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().userId()).isEqualTo(id);
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);
    }
}
