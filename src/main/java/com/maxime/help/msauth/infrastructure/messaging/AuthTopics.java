package com.maxime.help.msauth.infrastructure.messaging;

/** Names of the Kafka topics this service publishes to: part of its public contract. */
final class AuthTopics {

    static final String USER_REGISTERED = "auth.user.registered";
    static final String USER_SETTINGS_CHANGED = "auth.user.settings-changed";
    static final String PASSWORD_CHANGED = "auth.password.changed";
    static final String LOGIN_FAILED = "auth.login.failed";
    static final String LOGIN_SUCCEEDED = "auth.login.succeeded";
    static final String TOKEN_REFRESHED = "auth.token.refreshed";
    static final String LOGGED_OUT = "auth.logout";

    private AuthTopics() {}
}
