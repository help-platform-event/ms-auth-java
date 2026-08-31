package com.maxime.help.msauth.domain.model;

/**
 * Application role carried in the JWT. Only {@code USER} exists today; add values as the
 * domain grows. Pure domain type — persistence maps it to a string on its own side.
 */
public enum Role {
    USER
}
