package com.maxime.help.msauth.domain.port.out;

/**
 * Thrown by {@link GoogleIdentityProvider} when an authorization code cannot be exchanged for a
 * verified Google identity. Lives here rather than in {@code application} because
 * {@code infrastructure} may only depend on {@code domain} — this is the type an infrastructure
 * adapter throws that the web layer's exception handling can still catch.
 */
public class GoogleAuthenticationException extends RuntimeException {

    public GoogleAuthenticationException(String message) {
        super(message);
    }

    public GoogleAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
