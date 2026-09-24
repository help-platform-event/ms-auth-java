package com.maxime.help.msauth.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maxime.help.msauth.application.service.UserDirectoryService;
import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.web.dto.UserSummaryResponse;

import jakarta.validation.constraints.Size;

/**
 * User lookups for the Gateway. Listing every user is ADMIN-only — enforced in {@code
 * SecurityConfig}, next to the other path rules.
 */
@RestController
@RequestMapping("/api/users")
@SuppressWarnings("unused")
class UserController {

    private static final int MAX_BATCH_SIZE = 100;

    private final UserDirectoryService userDirectoryService;

    UserController(UserDirectoryService userDirectoryService) {
        this.userDirectoryService = userDirectoryService;
    }

    @GetMapping
    List<UserSummaryResponse> findAll() {
        return userDirectoryService.findAll().stream().map(UserController::toResponse).toList();
    }

    /** Batch lookup, e.g. {@code ?ids=a,b,c}. Unknown ids are silently omitted from the result. */
    @GetMapping("/profiles")
    List<UserSummaryResponse> findByIds(@RequestParam @Size(max = MAX_BATCH_SIZE) List<UUID> ids) {
        return userDirectoryService.findByIds(ids).stream().map(UserController::toResponse).toList();
    }

    @GetMapping("/{id}")
    UserSummaryResponse findById(@PathVariable UUID id) {
        return toResponse(userDirectoryService.findById(id));
    }

    private static UserSummaryResponse toResponse(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getProfile().getFirstName(),
                user.getProfile().getLastName(),
                user.getProfile().getAvatarUrl());
    }
}
