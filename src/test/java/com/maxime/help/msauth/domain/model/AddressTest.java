package com.maxime.help.msauth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Pure domain test — no Spring, no database. */
class AddressTest {

    @Test
    void completeAddress_isAccepted() {
        Address address = new Address(" 12 ", "Rue de Paris", null, "Lille", "59000", "France", null, null);

        assertThat(address.streetNumber()).isEqualTo("12");
        assertThat(address.isEmpty()).isFalse();
    }

    @Test
    void blankFields_areNormalisedToNullAndMakeAnEmptyAddress() {
        Address address = new Address("", "  ", null, "", "", "", null, null);

        assertThat(address.isEmpty()).isTrue();
        assertThat(address.streetName()).isNull();
    }

    @Test
    void partialAddress_isRejected() {
        assertThatThrownBy(() -> new Address("12", "Rue de Paris", null, "Lille", null, "France", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postalCode");
    }

    @Test
    void addressLine2Alone_isRejected() {
        assertThatThrownBy(() -> new Address(null, null, "Bât. B", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coordinatesWithoutText_areAllowed() {
        Address address = new Address(null, null, null, null, null, null, new BigDecimal("50.6"), new BigDecimal("3.06"));

        assertThat(address.isEmpty()).isFalse();
    }

    @Test
    void halfACoordinatePair_isRejected() {
        assertThatThrownBy(() -> new Address(
                        "12", "Rue de Paris", null, "Lille", "59000", "France", new BigDecimal("50.6"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude");
    }
}
