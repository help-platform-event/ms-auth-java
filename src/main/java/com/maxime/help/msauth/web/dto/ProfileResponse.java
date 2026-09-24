package com.maxime.help.msauth.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The caller's profile. Same shape as the NestJS service returned: {@code firstName}/{@code
 * lastName} are always present ({@code ""} when unset), other unset fields are omitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileResponse(
        String firstName, String lastName, String avatarUrl, String phone, String bio, AddressDto address) {}
