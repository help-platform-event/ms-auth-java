package com.maxime.help.msauth.domain.model;

/** Days of the week on which a user is generally available. Immutable value object. */
public record Availability(
        boolean monday,
        boolean tuesday,
        boolean wednesday,
        boolean thursday,
        boolean friday,
        boolean saturday,
        boolean sunday) {

    /** Available every day — the default for a user who never set their availability. */
    public static Availability allDays() {
        return new Availability(true, true, true, true, true, true, true);
    }
}
