package org.arghyam.jalsoochak.user.exceptions;

public class KeycloakOperationException extends RuntimeException {
    private final Integer statusCode;

    public KeycloakOperationException(String message) {
        super(message);
        this.statusCode = null;
    }

    public KeycloakOperationException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public KeycloakOperationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
