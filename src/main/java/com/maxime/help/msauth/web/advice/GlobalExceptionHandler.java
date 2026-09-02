package com.maxime.help.msauth.web.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.maxime.help.msauth.application.exception.AuthApplicationException;
import com.maxime.help.msauth.domain.port.out.GoogleAuthenticationException;

@RestControllerAdvice
@SuppressWarnings("unused")
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthApplicationException.class)
    ProblemDetail handleAuthApplicationException(AuthApplicationException ex) {
        return ProblemDetail.forStatusAndDetail(ex.httpStatus(), ex.getMessage());
    }

    @ExceptionHandler(GoogleAuthenticationException.class)
    ProblemDetail handleGoogleAuthenticationException(GoogleAuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Google authentication failed");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ProblemDetail handleDomainValidation(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
