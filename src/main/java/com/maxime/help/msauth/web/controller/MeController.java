package com.maxime.help.msauth.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maxime.help.msauth.application.service.UserProfileService;
import com.maxime.help.msauth.application.service.UserSettingsService;
import com.maxime.help.msauth.domain.model.Address;
import com.maxime.help.msauth.domain.model.Availability;
import com.maxime.help.msauth.domain.model.NotificationSettings;
import com.maxime.help.msauth.domain.model.Profile;
import com.maxime.help.msauth.domain.port.out.AccessTokenClaims;
import com.maxime.help.msauth.web.dto.AddressDto;
import com.maxime.help.msauth.web.dto.AvailabilityDto;
import com.maxime.help.msauth.web.dto.CoordinatesDto;
import com.maxime.help.msauth.web.dto.NotificationSettingsDto;
import com.maxime.help.msauth.web.dto.ProfileResponse;
import com.maxime.help.msauth.web.dto.UpdateProfileRequest;

import jakarta.validation.Valid;

/** The authenticated caller's own profile and settings. */
@RestController
@RequestMapping("/api/me")
@SuppressWarnings("unused")
class MeController {

    private final UserProfileService userProfileService;
    private final UserSettingsService userSettingsService;

    MeController(UserProfileService userProfileService, UserSettingsService userSettingsService) {
        this.userProfileService = userProfileService;
        this.userSettingsService = userSettingsService;
    }

    @GetMapping("/profile")
    ProfileResponse getProfile(@AuthenticationPrincipal AccessTokenClaims principal) {
        return toResponse(userProfileService.getProfile(principal.userId()));
    }

    @PatchMapping("/profile")
    ProfileResponse updateProfile(
            @AuthenticationPrincipal AccessTokenClaims principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return toResponse(userProfileService.updateProfile(
                principal.userId(),
                request.firstName(),
                request.lastName(),
                request.avatarUrl(),
                request.phone(),
                request.bio(),
                toDomain(request.address())));
    }

    @GetMapping("/availability")
    AvailabilityDto getAvailability(@AuthenticationPrincipal AccessTokenClaims principal) {
        return toDto(userSettingsService.getAvailability(principal.userId()));
    }

    @PatchMapping("/availability")
    AvailabilityDto updateAvailability(
            @AuthenticationPrincipal AccessTokenClaims principal, @Valid @RequestBody AvailabilityDto request) {
        return toDto(userSettingsService.updateAvailability(principal.userId(), toDomain(request)));
    }

    @GetMapping("/notifications")
    NotificationSettingsDto getNotifications(@AuthenticationPrincipal AccessTokenClaims principal) {
        return toDto(userSettingsService.getNotifications(principal.userId()));
    }

    @PatchMapping("/notifications")
    NotificationSettingsDto updateNotifications(
            @AuthenticationPrincipal AccessTokenClaims principal,
            @Valid @RequestBody NotificationSettingsDto request) {
        return toDto(userSettingsService.updateNotifications(principal.userId(), toDomain(request)));
    }

    private static ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.getFirstName() != null ? profile.getFirstName() : "",
                profile.getLastName() != null ? profile.getLastName() : "",
                profile.getAvatarUrl(),
                profile.getPhone(),
                profile.getBio(),
                toDto(profile.getAddress()));
    }

    private static AddressDto toDto(Address address) {
        if (address == null) {
            return null;
        }
        CoordinatesDto coordinates = address.latitude() != null
                ? new CoordinatesDto(address.latitude(), address.longitude())
                : null;
        return new AddressDto(
                address.streetNumber(),
                address.streetName(),
                address.addressLine2(),
                address.city(),
                address.postalCode(),
                address.country(),
                coordinates);
    }

    private static Address toDomain(AddressDto dto) {
        if (dto == null) {
            return null;
        }
        CoordinatesDto coordinates = dto.coordinates();
        return new Address(
                dto.streetNumber(),
                dto.streetName(),
                dto.addressLine2(),
                dto.city(),
                dto.postalCode(),
                dto.country(),
                coordinates != null ? coordinates.lat() : null,
                coordinates != null ? coordinates.lon() : null);
    }

    private static AvailabilityDto toDto(Availability a) {
        return new AvailabilityDto(
                a.monday(), a.tuesday(), a.wednesday(), a.thursday(), a.friday(), a.saturday(), a.sunday());
    }

    private static Availability toDomain(AvailabilityDto dto) {
        return new Availability(
                dto.monday(),
                dto.tuesday(),
                dto.wednesday(),
                dto.thursday(),
                dto.friday(),
                dto.saturday(),
                dto.sunday());
    }

    private static NotificationSettingsDto toDto(NotificationSettings n) {
        return new NotificationSettingsDto(
                n.enabled(),
                n.eventActivity(),
                n.eventMessages(),
                n.documents(),
                n.deadlines(),
                n.nearbyEvents(),
                n.judgments());
    }

    private static NotificationSettings toDomain(NotificationSettingsDto dto) {
        return new NotificationSettings(
                dto.enabled(),
                dto.eventActivity(),
                dto.eventMessages(),
                dto.documents(),
                dto.deadlines(),
                dto.nearbyEvents(),
                dto.judgments());
    }
}
