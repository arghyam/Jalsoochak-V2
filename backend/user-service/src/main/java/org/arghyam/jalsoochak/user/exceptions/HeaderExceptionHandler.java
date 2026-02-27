package org.arghyam.jalsoochak.user.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class HeaderExceptionHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingHeader(MissingRequestHeaderException ex) {
        if ("X-Tenant-Code".equals(ex.getHeaderName())) {
            return ResponseEntity
                    .badRequest()
                    .body("Tenant code is required");
        }
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
