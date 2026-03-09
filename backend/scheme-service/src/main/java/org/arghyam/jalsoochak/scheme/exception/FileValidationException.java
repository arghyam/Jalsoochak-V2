package org.arghyam.jalsoochak.scheme.exception;

import org.arghyam.jalsoochak.scheme.dto.SchemeUploadErrorDTO;

import java.util.List;

public class FileValidationException extends RuntimeException {

    private final List<SchemeUploadErrorDTO> errors;

    public FileValidationException(String message, List<SchemeUploadErrorDTO> errors) {
        super(message);
        this.errors = errors;
    }

    public List<SchemeUploadErrorDTO> getErrors() {
        return errors;
    }
}
