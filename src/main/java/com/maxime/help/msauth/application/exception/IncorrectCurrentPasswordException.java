package com.maxime.help.msauth.application.exception;

import org.springframework.http.HttpStatus;

public final class IncorrectCurrentPasswordException extends AuthApplicationException {

    public IncorrectCurrentPasswordException() {
        super("Current password is incorrect");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
