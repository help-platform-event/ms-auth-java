package com.maxime.help.msauth.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CoordinatesDto(@NotNull BigDecimal lat, @NotNull BigDecimal lon) {}
