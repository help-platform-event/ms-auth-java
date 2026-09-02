package com.maxime.help.msauth.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.maxime.help.msauth.application.service.AuthenticationService;
import com.maxime.help.msauth.application.service.TokenPair;
import com.maxime.help.msauth.domain.port.out.AccessTokenClaims;
import com.maxime.help.msauth.web.dto.ChangePasswordRequest;
import com.maxime.help.msauth.web.dto.GoogleLoginRequest;
import com.maxime.help.msauth.web.dto.LogoutRequest;
import com.maxime.help.msauth.web.dto.RefreshRequest;
import com.maxime.help.msauth.web.dto.SigninRequest;
import com.maxime.help.msauth.web.dto.SignupRequest;
import com.maxime.help.msauth.web.dto.TokenPairResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@SuppressWarnings("unused")
class AuthController {

    private final AuthenticationService authenticationService;

    AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    void signup(@Valid @RequestBody SignupRequest request) {
        authenticationService.signup(
                request.email(), request.password(), request.firstName(), request.lastName());
    }

    @PostMapping("/login")
    TokenPairResponse login(@Valid @RequestBody SigninRequest request) {
        return toResponse(authenticationService.login(request.email(), request.password()));
    }

    @PostMapping("/google")
    TokenPairResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        return toResponse(authenticationService.loginWithGoogle(request.code()));
    }

    @PostMapping("/refresh")
    TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return toResponse(authenticationService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@AuthenticationPrincipal AccessTokenClaims principal, @Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(principal.userId(), request.refreshToken());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(
            @AuthenticationPrincipal AccessTokenClaims principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(
                principal.userId(), request.currentPassword(), request.newPassword());
    }

    private static TokenPairResponse toResponse(TokenPair tokenPair) {
        return new TokenPairResponse(tokenPair.accessToken(), tokenPair.refreshToken());
    }
}
