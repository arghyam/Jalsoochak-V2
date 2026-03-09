package org.arghyam.jalsoochak.tenant.exception;

/**
 * General exception for configuration-related errors.
 */
public class ConfigurationException extends RuntimeException {
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
