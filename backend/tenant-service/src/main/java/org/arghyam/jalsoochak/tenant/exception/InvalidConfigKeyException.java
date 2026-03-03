package org.arghyam.jalsoochak.tenant.exception;

/**
 * Exception thrown when an invalid configuration key is provided.
 */
public class InvalidConfigKeyException extends RuntimeException {
    public InvalidConfigKeyException(String message) {
        super(message);
    }

    public InvalidConfigKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
