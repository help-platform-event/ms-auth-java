package com.maxime.help.msauth.application.exception;

import org.springframework.http.HttpStatus;

/** Defensive-only: an authenticated caller's user id no longer exists. Should never fire in practice. */
public final class UserNotFoundException extends AuthApplicationException {

    public UserNotFoundException() {
        super("User not found");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
