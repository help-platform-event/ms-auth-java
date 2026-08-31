package com.maxime.help.msauth.application.exception;

import org.springframework.http.HttpStatus;

/** Deliberately generic: used for both "no such user" and "wrong password" — no user enumeration. */
public final class InvalidCredentialsException extends AuthApplicationException {

    public InvalidCredentialsException() {
        super("Email or password is incorrect");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
