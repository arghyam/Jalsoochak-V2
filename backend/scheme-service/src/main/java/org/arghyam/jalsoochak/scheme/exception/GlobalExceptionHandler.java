package org.arghyam.jalsoochak.scheme.exception;

import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.scheme.dto.ApiErrorResponseDTO;
import org.arghyam.jalsoochak.scheme.dto.SchemeUploadErrorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleFileValidation(FileValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrors());
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleUnsupportedType(UnsupportedFileTypeException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDTO> handleGenericException(Exception ex) {
        log.error("Unexpected error while processing request", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", List.of());
    }

    private ResponseEntity<ApiErrorResponseDTO> build(
            HttpStatus status,
            String message,
            List<SchemeUploadErrorDTO> errors
    ) {
        ApiErrorResponseDTO body = ApiErrorResponseDTO.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .errors(errors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
