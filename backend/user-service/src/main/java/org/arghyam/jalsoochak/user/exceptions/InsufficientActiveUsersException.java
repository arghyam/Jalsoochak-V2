package org.arghyam.jalsoochak.user.exceptions;

public class InsufficientActiveUsersException extends RuntimeException {
    public InsufficientActiveUsersException(String message) {
        super(message);
    }
}
