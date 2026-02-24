package org.arghyam.jalsoochak.tenant.exception;

/**
 * Generic exception to be thrown when any resource (Tenant, User, Department,
 * etc.)
 * is not found. Mapped to HTTP 404 in GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
