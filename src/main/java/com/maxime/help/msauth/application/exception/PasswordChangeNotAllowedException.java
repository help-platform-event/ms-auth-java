package com.maxime.help.msauth.application.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a change-password attempt targets an OAuth-only account (no local password set). */
public final class PasswordChangeNotAllowedException extends AuthApplicationException {

    public PasswordChangeNotAllowedException() {
        super("This account has no password to change");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
