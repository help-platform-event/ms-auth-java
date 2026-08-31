package com.maxime.help.msauth.web.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(@NotBlank String code) {}
