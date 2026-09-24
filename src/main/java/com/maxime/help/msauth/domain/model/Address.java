package com.maxime.help.msauth.domain.model;

import java.math.BigDecimal;
import java.util.stream.Stream;

/**
 * Postal address value object. Immutable. Blank text is normalised to {@code null}.
 *
 * <p>An address is either empty or complete: as soon as any text field is set, street number,
 * street name, city, postal code and country are all required ({@code addressLine2} stays
 * optional). Coordinates are optional but come as a pair.
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

    public Address {
        streetNumber = blankToNull(streetNumber);
        streetName = blankToNull(streetName);
        addressLine2 = blankToNull(addressLine2);
        city = blankToNull(city);
        postalCode = blankToNull(postalCode);
        country = blankToNull(country);

        boolean hasText = Stream.of(streetNumber, streetName, addressLine2, city, postalCode, country)
                .anyMatch(value -> value != null);
        if (hasText) {
            requirePresent(streetNumber, "streetNumber");
            requirePresent(streetName, "streetName");
            requirePresent(city, "city");
            requirePresent(postalCode, "postalCode");
            requirePresent(country, "country");
        }
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("latitude and longitude must be provided together");
        }
    }

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

    private static void requirePresent(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("address." + field + " is required");
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
