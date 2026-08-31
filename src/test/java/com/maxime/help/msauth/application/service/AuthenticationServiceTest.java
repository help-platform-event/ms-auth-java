package com.maxime.help.msauth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maxime.help.msauth.application.exception.EmailAlreadyRegisteredException;
import com.maxime.help.msauth.application.exception.IncorrectCurrentPasswordException;
import com.maxime.help.msauth.application.exception.InvalidCredentialsException;
import com.maxime.help.msauth.application.exception.InvalidRefreshTokenException;
import com.maxime.help.msauth.application.exception.PasswordChangeNotAllowedException;
import com.maxime.help.msauth.domain.model.RefreshToken;
import com.maxime.help.msauth.domain.model.Role;
import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.domain.port.out.AccessTokenIssuer;
import com.maxime.help.msauth.domain.port.out.GoogleIdentity;
import com.maxime.help.msauth.domain.port.out.GoogleIdentityProvider;
import com.maxime.help.msauth.domain.port.out.PasswordHasher;
import com.maxime.help.msauth.domain.port.out.RefreshTokenRepository;
import com.maxime.help.msauth.domain.port.out.TokenHasher;
import com.maxime.help.msauth.domain.port.out.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pure application-service test — every port is mocked, no Spring, no database. */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private TokenHasher tokenHasher;
    @Mock private AccessTokenIssuer accessTokenIssuer;
    @Mock private GoogleIdentityProvider googleIdentityProvider;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service =
                new AuthenticationService(
                        userRepository,
                        refreshTokenRepository,
                        passwordHasher,
                        tokenHasher,
                        accessTokenIssuer,
                        googleIdentityProvider,
                        clock,
                        Duration.ofDays(30));
    }

    private static User persistedUser(UUID id, String email, String passwordHash) {
        return User.reconstitute(
                id, email, passwordHash, Role.USER, null, false, null, null, NOW, NOW);
    }

    /** Simulates what the real persistence adapter does: assigns an id to a brand-new user. */
    private static User asSaved(User user) {
        UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
        return User.reconstitute(
                id,
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getGoogleSub(),
                user.isTwoFactorEnabled(),
                user.getTwoFactorSecret(),
                user.getProfile(),
                NOW,
                NOW);
    }

    // --- signup ---

    @Test
    void signup_createsNewUserWhenEmailDoesNotExist() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordHasher.hash("raw-password")).thenReturn("hashed");
        UUID savedId = UUID.randomUUID();
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            assertThat(user.getEmail()).isEqualTo("alice@example.com");
                            assertThat(user.getPasswordHash()).isEqualTo("hashed");
                            assertThat(user.getProfile().getFirstName()).isEqualTo("Alice");
                            assertThat(user.getProfile().getLastName()).isEqualTo("Smith");
                            return User.reconstitute(
                                    savedId,
                                    user.getEmail(),
                                    user.getPasswordHash(),
                                    Role.USER,
                                    null,
                                    false,
                                    null,
                                    user.getProfile(),
                                    NOW,
                                    NOW);
                        });

        UUID result = service.signup("alice@example.com", "raw-password", "Alice", "Smith");

        assertThat(result).isEqualTo(savedId);
    }

    @Test
    void signup_throwsEmailAlreadyRegisteredWhenEmailExists() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.signup("alice@example.com", "raw-password", "Alice", "Smith"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
    }

    // --- login ---

    @Test
    void login_returnsTokensWhenCredentialsAreValid() {
        UUID userId = UUID.randomUUID();
        User user = persistedUser(userId, "alice@example.com", "hashed");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("raw-password", "hashed")).thenReturn(true);
        when(accessTokenIssuer.issue(userId, "alice@example.com", Role.USER)).thenReturn("access-token");
        when(tokenHasher.hash(anyString())).thenReturn("token-hash");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TokenPair result = service.login("alice@example.com", "raw-password");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNotBlank();
    }

    @Test
    void login_throwsInvalidCredentialsWhenUserDoesNotExist() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("nobody@example.com", "raw-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsInvalidCredentialsWhenPasswordIsIncorrect() {
        User user = persistedUser(UUID.randomUUID(), "alice@example.com", "hashed");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login("alice@example.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsInvalidCredentialsWhenAccountHasNoPassword() {
        User googleOnlyUser =
                User.reconstitute(
                        UUID.randomUUID(),
                        "bob@example.com",
                        null,
                        Role.USER,
                        "google-sub",
                        false,
                        null,
                        null,
                        NOW,
                        NOW);
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(googleOnlyUser));

        assertThatThrownBy(() -> service.login("bob@example.com", "any-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // --- refresh ---

    @Test
    void refresh_returnsNewTokensWhenRefreshTokenIsValid() {
        UUID userId = UUID.randomUUID();
        User user = persistedUser(userId, "alice@example.com", "hashed");
        RefreshToken stored =
                RefreshToken.reconstitute(
                        UUID.randomUUID(), userId, "old-hash", NOW.plusSeconds(60), false, NOW);
        when(tokenHasher.hash(anyString())).thenReturn("old-hash", "new-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accessTokenIssuer.issue(userId, "alice@example.com", Role.USER)).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TokenPair result = service.refresh("raw-refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).isRevoked()).isTrue();
        assertThat(captor.getAllValues().get(1).getId()).isNull();
    }

    @Test
    void refresh_throwsInvalidRefreshTokenWhenTokenNotFound() {
        when(tokenHasher.hash("unknown-token")).thenReturn("unknown-hash");
        when(refreshTokenRepository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_throwsInvalidRefreshTokenWhenUserNoLongerExists() {
        UUID userId = UUID.randomUUID();
        RefreshToken stored =
                RefreshToken.reconstitute(
                        UUID.randomUUID(), userId, "hash", NOW.plusSeconds(60), false, NOW);
        when(tokenHasher.hash("raw-refresh-token")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("raw-refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_throwsInvalidRefreshTokenWhenTokenIsRevokedOrExpired() {
        UUID userId = UUID.randomUUID();
        RefreshToken revoked =
                RefreshToken.reconstitute(
                        UUID.randomUUID(), userId, "hash", NOW.plusSeconds(60), true, NOW);
        when(tokenHasher.hash("raw-refresh-token")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.refresh("raw-refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    // --- logout ---

    @Test
    void logout_revokesOnlyTheMatchingTokenForTheCaller() {
        UUID userId = UUID.randomUUID();
        RefreshToken stored =
                RefreshToken.reconstitute(
                        UUID.randomUUID(), userId, "hash", NOW.plusSeconds(60), false, NOW);
        when(tokenHasher.hash("raw-refresh-token")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.logout(userId, "raw-refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().isRevoked()).isTrue();
    }

    @Test
    void logout_isIdempotentWhenTokenAlreadyRevokedOrNotFound() {
        when(tokenHasher.hash("unknown-token")).thenReturn("unknown-hash");
        when(refreshTokenRepository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());

        service.logout(UUID.randomUUID(), "unknown-token");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_isIdempotentWhenTokenBelongsToAnotherUser() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        RefreshToken stored =
                RefreshToken.reconstitute(
                        UUID.randomUUID(), ownerId, "hash", NOW.plusSeconds(60), false, NOW);
        when(tokenHasher.hash("raw-refresh-token")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(stored));

        service.logout(callerId, "raw-refresh-token");

        verify(refreshTokenRepository, never()).save(any());
    }

    // --- changePassword ---

    @Test
    void changePassword_updatesPasswordWhenCurrentPasswordIsValid() {
        UUID userId = UUID.randomUUID();
        User user = persistedUser(userId, "alice@example.com", "old-hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("current-password", "old-hash")).thenReturn(true);
        when(passwordHasher.hash("new-password")).thenReturn("new-hash");

        service.changePassword(userId, "current-password", "new-password");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void changePassword_throwsPasswordChangeNotAllowedWhenAccountHasNoPassword() {
        UUID userId = UUID.randomUUID();
        User googleOnlyUser =
                User.reconstitute(
                        userId, "bob@example.com", null, Role.USER, "google-sub", false, null, null, NOW, NOW);
        when(userRepository.findById(userId)).thenReturn(Optional.of(googleOnlyUser));

        assertThatThrownBy(() -> service.changePassword(userId, "current-password", "new-password"))
                .isInstanceOf(PasswordChangeNotAllowedException.class);
    }

    @Test
    void changePassword_throwsIncorrectCurrentPasswordWhenCurrentPasswordIsWrong() {
        UUID userId = UUID.randomUUID();
        User user = persistedUser(userId, "alice@example.com", "old-hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong-current-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(
                        () -> service.changePassword(userId, "wrong-current-password", "new-password"))
                .isInstanceOf(IncorrectCurrentPasswordException.class);
    }

    // --- loginWithGoogle ---

    @Test
    void loginWithGoogle_returnsTokensForExistingGoogleUser() {
        UUID userId = UUID.randomUUID();
        User googleUser =
                User.reconstitute(
                        userId, "alice@example.com", null, Role.USER, "google-sub", false, null, null, NOW, NOW);
        when(googleIdentityProvider.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new GoogleIdentity("google-sub", "alice@example.com", true));
        when(userRepository.findByGoogleSub("google-sub")).thenReturn(Optional.of(googleUser));
        when(accessTokenIssuer.issue(userId, "alice@example.com", Role.USER)).thenReturn("access-token");
        when(tokenHasher.hash(anyString())).thenReturn("token-hash");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TokenPair result = service.loginWithGoogle("auth-code");

        assertThat(result.accessToken()).isEqualTo("access-token");
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithGoogle_backfillsGoogleSubOntoExistingPasswordAccountMatchedByEmail() {
        UUID userId = UUID.randomUUID();
        User existingPasswordUser = persistedUser(userId, "alice@example.com", "hashed");
        when(googleIdentityProvider.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new GoogleIdentity("google-sub", "alice@example.com", true));
        when(userRepository.findByGoogleSub("google-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingPasswordUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accessTokenIssuer.issue(any(), anyString(), any())).thenReturn("access-token");
        when(tokenHasher.hash(anyString())).thenReturn("token-hash");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.loginWithGoogle("auth-code");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getGoogleSub()).isEqualTo("google-sub");
    }

    @Test
    void loginWithGoogle_doesNotLinkWhenEmailUnverifiedAndCreatesNewAccountInstead() {
        when(googleIdentityProvider.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new GoogleIdentity("google-sub", "alice@example.com", false));
        when(userRepository.findByGoogleSub("google-sub")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> asSaved(invocation.getArgument(0)));
        when(accessTokenIssuer.issue(any(), anyString(), any())).thenReturn("access-token");
        when(tokenHasher.hash(anyString())).thenReturn("token-hash");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.loginWithGoogle("auth-code");

        verify(userRepository, never()).findByEmail(anyString());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getGoogleSub()).isEqualTo("google-sub");
        assertThat(captor.getValue().hasPassword()).isFalse();
    }

    @Test
    void loginWithGoogle_createsNewGoogleOnlyUserWhenNoExistingAccountMatches() {
        when(googleIdentityProvider.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new GoogleIdentity("google-sub", "new@example.com", true));
        when(userRepository.findByGoogleSub("google-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> asSaved(invocation.getArgument(0)));
        when(accessTokenIssuer.issue(any(), anyString(), any())).thenReturn("access-token");
        when(tokenHasher.hash(anyString())).thenReturn("token-hash");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.loginWithGoogle("auth-code");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getGoogleSub()).isEqualTo("google-sub");
    }
}
