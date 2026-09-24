package com.maxime.help.msauth.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;

/** Postal address as exchanged with the Gateway; completeness is enforced by the domain. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AddressDto(
        String streetNumber,
        String streetName,
        String addressLine2,
        String city,
        String postalCode,
        String country,
        @Valid CoordinatesDto coordinates) {}
