package org.arghyam.jalsoochak.tenant.exception;

/**
 * Exception thrown when a configuration value is invalid or malformed.
 */
public class InvalidConfigValueException extends RuntimeException {
    public InvalidConfigValueException(String message) {
        super(message);
    }

    public InvalidConfigValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
