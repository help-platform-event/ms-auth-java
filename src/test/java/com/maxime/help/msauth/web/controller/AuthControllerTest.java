package com.maxime.help.msauth.web.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maxime.help.msauth.application.exception.InvalidCredentialsException;
import com.maxime.help.msauth.application.service.AuthenticationService;
import com.maxime.help.msauth.application.service.TokenPair;
import com.maxime.help.msauth.domain.model.Role;
import com.maxime.help.msauth.domain.port.out.AccessTokenClaims;
import com.maxime.help.msauth.domain.port.out.AccessTokenIssuer;
import com.maxime.help.msauth.infrastructure.security.SecurityConfig;
import com.maxime.help.msauth.web.advice.GlobalExceptionHandler;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthenticationService authenticationService;
    @MockitoBean private AccessTokenIssuer accessTokenIssuer;

    @Test
    void signup_isReachableWithoutAuthentication() throws Exception {
        mockMvc
                .perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"alice@example.com","password":"ab12!!cd","firstName":"Alice","lastName":"Smith"}
                                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void login_returnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        when(authenticationService.login(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc
                .perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"alice@example.com","password":"wrong-password"}
                                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_isRejectedWithoutABearerToken() throws Exception {
        mockMvc
                .perform(
                        post("/api/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"refreshToken":"some-refresh-token"}
                                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_succeedsWithAValidBearerToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(accessTokenIssuer.parse("valid-access-token"))
                .thenReturn(Optional.of(new AccessTokenClaims(userId, "alice@example.com", Role.USER)));

        mockMvc
                .perform(
                        post("/api/auth/logout")
                                .header("Authorization", "Bearer valid-access-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"refreshToken":"some-refresh-token"}
                                        """))
                .andExpect(status().isNoContent());
    }

    @Test
    void refresh_returnsTokenPairOnSuccess() throws Exception {
        when(authenticationService.refresh("a-refresh-token"))
                .thenReturn(new TokenPair("new-access-token", "new-refresh-token"));

        mockMvc
                .perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"refreshToken":"a-refresh-token"}
                                        """))
                .andExpect(status().isOk());
    }

    @Test
    void signup_returnsBadRequestForWeakPassword() throws Exception {
        mockMvc
                .perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"alice@example.com","password":"weak","firstName":"Alice","lastName":"Smith"}
                                        """))
                .andExpect(status().isBadRequest());
    }
}
