package com.maxime.help.msauth.application.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maxime.help.msauth.application.exception.EmailAlreadyRegisteredException;
import com.maxime.help.msauth.application.exception.IncorrectCurrentPasswordException;
import com.maxime.help.msauth.application.exception.InvalidCredentialsException;
import com.maxime.help.msauth.application.exception.InvalidRefreshTokenException;
import com.maxime.help.msauth.application.exception.PasswordChangeNotAllowedException;
import com.maxime.help.msauth.application.exception.UserNotFoundException;
import com.maxime.help.msauth.domain.model.RefreshToken;
import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.domain.port.out.AccessTokenIssuer;
import com.maxime.help.msauth.domain.port.out.GoogleIdentity;
import com.maxime.help.msauth.domain.port.out.GoogleIdentityProvider;
import com.maxime.help.msauth.domain.port.out.PasswordHasher;
import com.maxime.help.msauth.domain.port.out.RefreshTokenRepository;
import com.maxime.help.msauth.domain.port.out.TokenHasher;
import com.maxime.help.msauth.domain.port.out.UserRepository;

/**
 * Orchestrates signup, login (password and Google), refresh-token rotation, logout, and
 * change-password. No business logic here — it lives in the domain model; this class wires
 * domain objects to outbound ports.
 */
@Service
public class AuthenticationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final TokenHasher tokenHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final GoogleIdentityProvider googleIdentityProvider;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    AuthenticationService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasher passwordHasher,
            TokenHasher tokenHasher,
            AccessTokenIssuer accessTokenIssuer,
            GoogleIdentityProvider googleIdentityProvider,
            Clock clock,
            @Value("${app.auth.refresh-token.ttl}") Duration refreshTokenTtl) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.tokenHasher = tokenHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.googleIdentityProvider = googleIdentityProvider;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public UUID signup(String email, String rawPassword, String firstName, String lastName) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = User.register(email, passwordHasher.hash(rawPassword));
        user.getProfile().changeName(firstName, lastName);
        return userRepository.save(user).getId();
    }

    @Transactional
    public TokenPair login(String email, String rawPassword) {
        User user =
                userRepository
                        .findByEmail(email)
                        .filter(User::hasPassword)
                        .orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueTokenPair(user);
    }

    @Transactional
    public TokenPair loginWithGoogle(String authorizationCode) {
        GoogleIdentity identity = googleIdentityProvider.exchangeAuthorizationCode(authorizationCode);

        User user =
                userRepository
                        .findByGoogleSub(identity.sub())
                        .or(() -> linkIfVerifiedEmailMatch(identity))
                        .orElseGet(
                                () ->
                                        userRepository.save(
                                                User.registerWithGoogle(identity.email(), identity.sub())));
        return issueTokenPair(user);
    }

    private Optional<User> linkIfVerifiedEmailMatch(GoogleIdentity identity) {
        if (!identity.emailVerified()) {
            return Optional.empty();
        }
        return userRepository
                .findByEmail(identity.email())
                .map(
                        existing -> {
                            existing.linkGoogleAccount(identity.sub());
                            return userRepository.save(existing);
                        });
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        String hash = tokenHasher.hash(rawRefreshToken);
        RefreshToken stored =
                refreshTokenRepository
                        .findByTokenHash(hash)
                        .filter(token -> token.isActive(clock.instant()))
                        .orElseThrow(InvalidRefreshTokenException::new);
        User user =
                userRepository
                        .findById(stored.getUserId())
                        .orElseThrow(InvalidRefreshTokenException::new);

        stored.revoke();
        refreshTokenRepository.save(stored);

        return issueTokenPair(user);
    }

    @Transactional
    public void logout(UUID callerId, String rawRefreshToken) {
        String hash = tokenHasher.hash(rawRefreshToken);
        refreshTokenRepository
                .findByTokenHash(hash)
                .filter(token -> token.getUserId().equals(callerId))
                .ifPresent(
                        token -> {
                            token.revoke();
                            refreshTokenRepository.save(token);
                        });
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (!user.hasPassword()) {
            throw new PasswordChangeNotAllowedException();
        }
        if (!passwordHasher.matches(currentPassword, user.getPasswordHash())) {
            throw new IncorrectCurrentPasswordException();
        }
        user.changePassword(passwordHasher.hash(newPassword));
        userRepository.save(user);
    }

    private TokenPair issueTokenPair(User user) {
        String accessToken = accessTokenIssuer.issue(user.getId(), user.getEmail(), user.getRole());

        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        Instant expiresAt = clock.instant().plus(refreshTokenTtl);
        refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), tokenHasher.hash(rawRefreshToken), expiresAt));

        return new TokenPair(accessToken, rawRefreshToken);
    }
}
