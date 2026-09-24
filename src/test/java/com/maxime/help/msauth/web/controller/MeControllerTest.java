package com.maxime.help.msauth.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maxime.help.msauth.application.exception.UserNotFoundException;
import com.maxime.help.msauth.application.service.UserProfileService;
import com.maxime.help.msauth.application.service.UserSettingsService;
import com.maxime.help.msauth.domain.model.Address;
import com.maxime.help.msauth.domain.model.Availability;
import com.maxime.help.msauth.domain.model.NotificationSettings;
import com.maxime.help.msauth.domain.model.Profile;
import com.maxime.help.msauth.domain.model.Role;
import com.maxime.help.msauth.domain.port.out.AccessTokenClaims;
import com.maxime.help.msauth.domain.port.out.AccessTokenIssuer;
import com.maxime.help.msauth.infrastructure.security.SecurityConfig;
import com.maxime.help.msauth.web.advice.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MeControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String BEARER = "Bearer valid-access-token";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserProfileService userProfileService;
    @MockitoBean private UserSettingsService userSettingsService;
    @MockitoBean private AccessTokenIssuer accessTokenIssuer;

    @BeforeEach
    void authenticate() {
        when(accessTokenIssuer.parse("valid-access-token"))
                .thenReturn(Optional.of(new AccessTokenClaims(USER_ID, "alice@example.com", Role.USER)));
    }

    @Test
    void getProfile_isRejectedWithoutABearerToken() throws Exception {
        mockMvc.perform(get("/api/me/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_matchesTheNestJsShape() throws Exception {
        Address address = new Address(
                "12", "Rue de Paris", null, "Lille", "59000", "France", new BigDecimal("50.63"), new BigDecimal("3.06"));
        when(userProfileService.getProfile(USER_ID))
                .thenReturn(Profile.reconstitute(null, null, null, "0600000000", null, address));

        mockMvc.perform(get("/api/me/profile").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(""))
                .andExpect(jsonPath("$.lastName").value(""))
                .andExpect(jsonPath("$.phone").value("0600000000"))
                .andExpect(jsonPath("$.avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.address.city").value("Lille"))
                .andExpect(jsonPath("$.address.addressLine2").doesNotExist())
                .andExpect(jsonPath("$.address.coordinates.lat").value(50.63))
                .andExpect(jsonPath("$.address.coordinates.lon").value(3.06));
    }

    @Test
    void getProfile_returnsNotFoundForADeletedUser() throws Exception {
        when(userProfileService.getProfile(USER_ID)).thenThrow(new UserNotFoundException());

        mockMvc.perform(get("/api/me/profile").header("Authorization", BEARER)).andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_passesTheAddressWithCoordinatesToTheService() throws Exception {
        Address expected = new Address(
                "12", "Rue de Paris", null, "Lille", "59000", "France", new BigDecimal("50.63"), new BigDecimal("3.06"));
        when(userProfileService.updateProfile(
                        eq(USER_ID), eq("Alice"), eq("Smith"), isNull(), isNull(), isNull(), eq(expected)))
                .thenReturn(Profile.reconstitute("Alice", "Smith", null, null, null, expected));

        mockMvc.perform(patch("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Alice","lastName":"Smith",
                                 "address":{"streetNumber":"12","streetName":"Rue de Paris","city":"Lille",
                                            "postalCode":"59000","country":"France",
                                            "coordinates":{"lat":50.63,"lon":3.06}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.address.coordinates.lat").value(50.63));
    }

    @Test
    void updateProfile_rejectsAnIncompleteAddress() throws Exception {
        mockMvc.perform(patch("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Alice","lastName":"Smith","address":{"city":"Lille"}}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userProfileService);
    }

    @Test
    void updateProfile_rejectsBlankNamesAndAnInvalidPhone() throws Exception {
        mockMvc.perform(patch("/api/me/profile")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":" ","lastName":"Smith","phone":"abc"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userProfileService);
    }

    @Test
    void getAvailability_returnsEveryDay() throws Exception {
        when(userSettingsService.getAvailability(USER_ID))
                .thenReturn(new Availability(true, true, true, true, true, false, false));

        mockMvc.perform(get("/api/me/availability").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monday").value(true))
                .andExpect(jsonPath("$.sunday").value(false));
    }

    @Test
    void updateAvailability_rejectsAMissingDay() throws Exception {
        mockMvc.perform(patch("/api/me/availability")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monday":true,"tuesday":true,"wednesday":true,"thursday":true,"friday":true,"saturday":false}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userSettingsService);
    }

    @Test
    void updateNotifications_returnsTheSavedSettings() throws Exception {
        NotificationSettings muted = new NotificationSettings(false, true, true, true, true, true, false);
        when(userSettingsService.updateNotifications(eq(USER_ID), any())).thenReturn(muted);

        mockMvc.perform(patch("/api/me/notifications")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false,"eventActivity":true,"eventMessages":true,"documents":true,
                                 "deadlines":true,"nearbyEvents":true,"judgments":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.judgments").value(false));
    }
}
