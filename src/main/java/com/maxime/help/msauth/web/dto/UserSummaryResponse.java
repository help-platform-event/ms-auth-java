package com.maxime.help.msauth.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/** Public view of a user for other services. snake_case to match what the Gateway already consumes. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserSummaryResponse(
        UUID id,
        String email,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("avatar_url") String avatarUrl) {}
