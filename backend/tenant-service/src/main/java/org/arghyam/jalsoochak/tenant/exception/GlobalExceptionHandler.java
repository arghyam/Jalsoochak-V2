package org.arghyam.jalsoochak.tenant.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.tenant.dto.common.ApiErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication required: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, String>> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return Map.of("field", field, "message", v.getMessage());
                })
                .toList();
        log.warn("Constraint violation: {}", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    String message = Optional.ofNullable(fe.getDefaultMessage()).orElse("Validation failed");
                    return Map.of("field", fe.getField(), "message", message);
                })
                .toList();
        log.warn("Validation failed: {}", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleForbiddenAccess(ForbiddenAccessException ex) {
        log.warn("Forbidden access: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getParameter().getParameterName());
        log.warn("Type mismatch: {}", message);
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(InvalidConfigKeyException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleInvalidConfigKey(InvalidConfigKeyException ex) {
        log.warn("Invalid config key: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidConfigValueException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleInvalidConfigValue(InvalidConfigValueException ex) {
        log.warn("Invalid config value: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleConfigurationException(ConfigurationException ex) {
        log.error("Configuration error: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Configuration processing failed");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(LocationHierarchyStructureLockedException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleLocationHierarchyStructureLocked(
            LocationHierarchyStructureLockedException ex) {
        log.warn("Location hierarchy structure locked: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleConflict(IllegalStateException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected runtime error: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDTO> handleException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ApiErrorResponseDTO> build(HttpStatus status, String message) {
        return build(status, message, null);
    }

    private ResponseEntity<ApiErrorResponseDTO> build(HttpStatus status, String message, Object fieldErrors) {
        return ResponseEntity.status(status).body(
                new ApiErrorResponseDTO(status.value(), status.getReasonPhrase(), message, fieldErrors));
    }
}
