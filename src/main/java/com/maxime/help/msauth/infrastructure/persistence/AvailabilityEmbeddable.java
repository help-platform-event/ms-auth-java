package com.maxime.help.msauth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA embeddable mirroring the domain {@code Availability} value object. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class AvailabilityEmbeddable {

    @Column(name = "available_monday", nullable = false)
    private boolean monday;

    @Column(name = "available_tuesday", nullable = false)
    private boolean tuesday;

    @Column(name = "available_wednesday", nullable = false)
    private boolean wednesday;

    @Column(name = "available_thursday", nullable = false)
    private boolean thursday;

    @Column(name = "available_friday", nullable = false)
    private boolean friday;

    @Column(name = "available_saturday", nullable = false)
    private boolean saturday;

    @Column(name = "available_sunday", nullable = false)
    private boolean sunday;
}
