package com.maxime.help.msauth.application.exception;

import org.springframework.http.HttpStatus;

public final class EmailAlreadyRegisteredException extends AuthApplicationException {

    public EmailAlreadyRegisteredException() {
        super("An account already exists for this email");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }
}
