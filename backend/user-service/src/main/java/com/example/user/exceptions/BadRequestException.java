package com.example.user.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    private Object errors;

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Object errors) {
        super(message);
        this.errors = errors;
    }

    public Object getErrors() {
        return errors;
    }
}
