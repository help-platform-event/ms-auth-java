package com.maxime.help.msauth.application.exception;

import org.springframework.http.HttpStatus;

/** Base type for authentication use-case failures, each carrying its own HTTP status mapping. */
public sealed abstract class AuthApplicationException extends RuntimeException
        permits EmailAlreadyRegisteredException,
                InvalidCredentialsException,
                UserNotFoundException,
                InvalidRefreshTokenException,
                IncorrectCurrentPasswordException,
                PasswordChangeNotAllowedException {

    protected AuthApplicationException(String message) {
        super(message);
    }

    public abstract HttpStatus httpStatus();
}
