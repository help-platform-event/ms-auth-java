package com.maxime.help.msauth.web.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maxime.help.msauth.application.exception.UserNotFoundException;
import com.maxime.help.msauth.application.service.UserDirectoryService;
import com.maxime.help.msauth.domain.model.Profile;
import com.maxime.help.msauth.domain.model.Role;
import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.domain.port.out.AccessTokenClaims;
import com.maxime.help.msauth.domain.port.out.AccessTokenIssuer;
import com.maxime.help.msauth.infrastructure.security.SecurityConfig;
import com.maxime.help.msauth.web.advice.GlobalExceptionHandler;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    private static final String USER_BEARER = "Bearer user-token";
    private static final String ADMIN_BEARER = "Bearer admin-token";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserDirectoryService userDirectoryService;
    @MockitoBean private AccessTokenIssuer accessTokenIssuer;

    private final User alice = User.reconstitute(
            UUID.randomUUID(), "alice@example.com", "argon2-hash", Role.USER, null, false, null,
            Profile.reconstitute("Alice", "Smith", "http://img/alice.png", null, null, null), null, null);

    @BeforeEach
    void authenticate() {
        when(accessTokenIssuer.parse("user-token"))
                .thenReturn(Optional.of(new AccessTokenClaims(UUID.randomUUID(), "bob@example.com", Role.USER)));
        when(accessTokenIssuer.parse("admin-token"))
                .thenReturn(Optional.of(new AccessTokenClaims(UUID.randomUUID(), "root@example.com", Role.ADMIN)));
    }

    @Test
    void findById_returnsASnakeCaseSummaryWithoutTheHash() throws Exception {
        when(userDirectoryService.findById(alice.getId())).thenReturn(alice);

        mockMvc.perform(get("/api/users/{id}", alice.getId()).header("Authorization", USER_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alice.getId().toString()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.first_name").value("Alice"))
                .andExpect(jsonPath("$.last_name").value("Smith"))
                .andExpect(jsonPath("$.avatar_url").value("http://img/alice.png"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void findById_returnsNotFoundForAnUnknownUser() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(userDirectoryService.findById(unknown)).thenThrow(new UserNotFoundException());

        mockMvc.perform(get("/api/users/{id}", unknown).header("Authorization", USER_BEARER))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_isRejectedWithoutABearerToken() throws Exception {
        mockMvc.perform(get("/api/users/{id}", alice.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void findByIds_returnsTheKnownUsers() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(userDirectoryService.findByIds(List.of(alice.getId(), unknown))).thenReturn(List.of(alice));

        mockMvc.perform(get("/api/users/profiles")
                        .param("ids", alice.getId() + "," + unknown)
                        .header("Authorization", USER_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].first_name").value("Alice"));
    }

    @Test
    void findByIds_rejectsMoreThanOneHundredIds() throws Exception {
        String ids = IntStream.range(0, 101)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.joining(","));

        mockMvc.perform(get("/api/users/profiles").param("ids", ids).header("Authorization", USER_BEARER))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userDirectoryService);
    }

    @Test
    void findAll_isForbiddenForARegularUser() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", USER_BEARER)).andExpect(status().isForbidden());
        verifyNoInteractions(userDirectoryService);
    }

    @Test
    void findAll_isAllowedForAnAdmin() throws Exception {
        when(userDirectoryService.findAll()).thenReturn(List.of(alice));

        mockMvc.perform(get("/api/users").header("Authorization", ADMIN_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));
    }
}
