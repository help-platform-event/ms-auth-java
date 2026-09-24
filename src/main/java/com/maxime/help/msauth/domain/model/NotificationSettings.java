package com.maxime.help.msauth.domain.model;

/**
 * Which notifications a user wants to receive. {@code enabled} is the master switch; the other
 * flags select individual categories. Immutable value object.
 */
public record NotificationSettings(
        boolean enabled,
        boolean eventActivity,
        boolean eventMessages,
        boolean documents,
        boolean deadlines,
        boolean nearbyEvents,
        boolean judgments) {

    /** Everything on — the default for a user who never changed their notification settings. */
    public static NotificationSettings defaults() {
        return new NotificationSettings(true, true, true, true, true, true, true);
    }
}
