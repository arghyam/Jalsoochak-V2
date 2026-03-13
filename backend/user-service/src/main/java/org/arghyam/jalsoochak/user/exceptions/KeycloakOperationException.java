package org.arghyam.jalsoochak.user.exceptions;

public class KeycloakOperationException extends RuntimeException {
    public KeycloakOperationException(String message) {
        super(message);
    }

    public KeycloakOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
