package com.maxime.help.msauth.infrastructure.messaging;

/** Names of the Kafka topics this service publishes to: part of its public contract. */
final class AuthTopics {

    static final String USER_REGISTERED = "auth.user.registered";
    static final String USER_SETTINGS_CHANGED = "auth.user.settings-changed";
    static final String PASSWORD_CHANGED = "auth.password.changed";

    private AuthTopics() {}
}
