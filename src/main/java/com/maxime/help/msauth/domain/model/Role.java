package com.maxime.help.msauth.domain.model;

/**
 * Application role carried in the JWT. Pure domain type — persistence maps it to a string on its
 * own side.
 */
public enum Role {
    USER,
    ADMIN
}
