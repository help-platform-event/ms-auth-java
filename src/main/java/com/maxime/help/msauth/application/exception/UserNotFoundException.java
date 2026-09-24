package com.maxime.help.msauth.application.exception;

import org.springframework.http.HttpStatus;

/** The requested user does not exist — either a looked-up id, or an authenticated caller deleted since. */
public final class UserNotFoundException extends AuthApplicationException {

    public UserNotFoundException() {
        super("User not found");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
