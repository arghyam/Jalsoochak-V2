package org.arghyam.jalsoochak.tenant.exception;

/**
 * Exception to be thrown when a user attempts an operation they don't have
 * permission for.
 * Mapped to HTTP 403 Forbidden in GlobalExceptionHandler.
 */
public class ForbiddenAccessException extends RuntimeException {
    public ForbiddenAccessException(String message) {
        super(message);
    }
}
