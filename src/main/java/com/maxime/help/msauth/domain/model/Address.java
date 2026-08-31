package com.maxime.help.msauth.domain.model;

import java.math.BigDecimal;

/**
 * Postal address value object. Immutable; every field is optional.
 */
public record Address(
        String streetNumber,
        String streetName,
        String addressLine2,
        String city,
        String postalCode,
        String country,
        BigDecimal latitude,
        BigDecimal longitude) {

    public boolean isEmpty() {
        return streetNumber == null
                && streetName == null
                && addressLine2 == null
                && city == null
                && postalCode == null
                && country == null
                && latitude == null
                && longitude == null;
    }
}
