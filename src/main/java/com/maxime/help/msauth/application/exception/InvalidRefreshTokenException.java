package com.maxime.help.msauth.application.exception;

import org.springframework.http.HttpStatus;

/** One generic message covering not-found, expired, revoked, or orphaned refresh tokens. */
public final class InvalidRefreshTokenException extends AuthApplicationException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
