package com.example.user.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class HeaderExceptionHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingHeader(MissingRequestHeaderException ex) {
        if ("X-Tenant-ID".equals(ex.getHeaderName())) {
            return ResponseEntity
                    .badRequest()
                    .body("Tenant ID is required");
        }
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
