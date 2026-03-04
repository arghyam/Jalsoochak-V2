package org.arghyam.jalsoochak.tenant.exception;

import org.arghyam.jalsoochak.tenant.dto.common.ApiErrorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler.
 * Covers exception handling for various Spring Web validation and type mismatch scenarios.
 */
@DisplayName("Global Exception Handler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("Validation Error Handler Tests")
    class ValidationErrorHandlerTests {

        @Test
        @DisplayName("Should handle validation errors and return 400")
        void testHandleValidationErrors_Success() {
            // Arrange
            BindingResult bindingResult = mock(BindingResult.class);
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);

            List<FieldError> fieldErrors = new ArrayList<>();
            fieldErrors.add(new FieldError("object", "name", "Name is required"));
            fieldErrors.add(new FieldError("object", "email", "Invalid email format"));

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleValidationErrors(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatus());
            assertEquals("Bad Request", response.getBody().getError());
            assertTrue(response.getBody().getMessage().contains("name: Name is required"));
            assertTrue(response.getBody().getMessage().contains("email: Invalid email format"));
        }

        @Test
        @DisplayName("Should handle validation errors with no field errors")
        void testHandleValidationErrors_NoFieldErrors() {
            // Arrange
            BindingResult bindingResult = mock(BindingResult.class);
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(new ArrayList<>());

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleValidationErrors(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatus());
            assertEquals("", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Type Mismatch Handler Tests")
    class TypeMismatchHandlerTests {

        @Test
        @DisplayName("Should handle type mismatch and return bad request with correct message")
        void testHandleTypeMismatch_Success() {
            // Arrange
            MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
            when(ex.getValue()).thenReturn("abc");
            when(ex.getParameter()).thenReturn(mock(org.springframework.core.MethodParameter.class));
            when(ex.getParameter().getParameterName()).thenReturn("pageSize");

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleTypeMismatch(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatus());
            assertTrue(response.getBody().getMessage().contains("Invalid value"));
            assertTrue(response.getBody().getMessage().contains("pageSize"));
        }
    }

    @Nested
    @DisplayName("ResourceNotFoundException Handler Tests")
    class ResourceNotFoundHandlerTests {

        @Test
        @DisplayName("Should handle resource not found and return 404")
        void testHandleResourceNotFound_Success() {
            // Arrange
            ResourceNotFoundException ex = new ResourceNotFoundException("Tenant not found");

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleResourceNotFound(ex);

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().getStatus());
            assertEquals("Not Found", response.getBody().getError());
            assertEquals("Tenant not found", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("ForbiddenAccessException Handler Tests")
    class ForbiddenAccessHandlerTests {

        @Test
        @DisplayName("Should handle forbidden access and return 403")
        void testHandleForbiddenAccess_Success() {
            // Arrange
            ForbiddenAccessException ex = new ForbiddenAccessException("Access denied");

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleForbiddenAccess(ex);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(403, response.getBody().getStatus());
            assertEquals("Forbidden", response.getBody().getError());
            assertEquals("Access denied", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Configuration Exception Handler Tests")
    class ConfigurationExceptionHandlerTests {

        @Test
        @DisplayName("Should handle configuration exception and return 500 with hardcoded safe message")
        void testHandleConfigurationException_Success() {
            // Arrange — handler returns a hardcoded message regardless of ex.getMessage()
            ConfigurationException ex = new ConfigurationException("Internal details that must not leak");

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleConfigurationException(ex);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(500, response.getBody().getStatus());
            assertEquals("Internal Server Error", response.getBody().getError());
            assertEquals("Configuration processing failed", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException Handler Tests")
    class IllegalArgumentHandlerTests {

        @Test
        @DisplayName("Should handle IllegalArgumentException and return 400")
        void testHandleIllegalArgumentException_Success() {
            // Arrange
            IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleBadRequest(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatus());
            assertEquals("Bad Request", response.getBody().getError());
            assertEquals("Invalid argument provided", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("IllegalStateException Handler Tests")
    class IllegalStateHandlerTests {

        @Test
        @DisplayName("Should handle IllegalStateException and return 409 conflict")
        void testHandleIllegalStateException_Success() {
            // Arrange
            IllegalStateException ex = new IllegalStateException("Tenant already exists");

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleConflict(ex);

            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().getStatus());
            assertEquals("Conflict", response.getBody().getError());
            assertEquals("Tenant already exists", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Generic Exception Handler Tests")
    class GenericExceptionHandlerTests {

        @Test
        @DisplayName("Should handle generic exception and return 500 with hardcoded safe message")
        void testHandleException_Success() {
            // Arrange — handler returns a hardcoded message regardless of ex.getMessage()
            Exception ex = new Exception("Low-level internal detail that must not leak");

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleException(ex);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(500, response.getBody().getStatus());
            assertEquals("Internal Server Error", response.getBody().getError());
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should handle NullPointerException as a generic exception")
        void testHandleNullPointerException() {
            // Arrange
            NullPointerException ex = new NullPointerException("Null reference encountered");

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleException(ex);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(500, response.getBody().getStatus());
        }
    }

    @Nested
    @DisplayName("RuntimeException Handler Tests")
    class RuntimeExceptionHandlerTests {

        @Test
        @DisplayName("Should handle RuntimeException and return 500 with hardcoded safe message")
        void testHandleRuntimeException_Success() {
            // Arrange — handler returns a hardcoded message regardless of ex.getMessage()
            RuntimeException ex = new RuntimeException("Schema provisioning failed: connection timeout");

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleRuntimeException(ex);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(500, response.getBody().getStatus());
            assertEquals("Internal Server Error", response.getBody().getError());
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should handle IllegalStateException separately from RuntimeException handler")
        void testIllegalStateException_HandledByConflictHandler_NotRuntimeHandler() {
            // IllegalStateException is handled by handleConflict (409), not handleRuntimeException
            IllegalStateException ex = new IllegalStateException("Tenant already exists");
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleConflict(ex);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals(409, response.getBody().getStatus());
        }
    }

    @Nested
    @DisplayName("InvalidConfigKeyException Handler Tests")
    class InvalidConfigKeyHandlerTests {

        @Test
        @DisplayName("Should handle invalid config key exception and return 400")
        void testHandleInvalidConfigKey_Success() {
            // Arrange
            InvalidConfigKeyException ex = new InvalidConfigKeyException("Invalid config key: UNKNOWN_KEY");

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleInvalidConfigKey(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatus());
            assertEquals("Bad Request", response.getBody().getError());
            assertEquals("Invalid config key: UNKNOWN_KEY", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("InvalidConfigValueException Handler Tests")
    class InvalidConfigValueHandlerTests {

        @Test
        @DisplayName("Should handle invalid config value exception and return 400")
        void testHandleInvalidConfigValue_Success() {
            // Arrange
            InvalidConfigValueException ex = new InvalidConfigValueException("Invalid config value format");

            // Act
            ResponseEntity<ApiErrorResponseDTO> response = handler.handleInvalidConfigValue(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatus());
            assertEquals("Bad Request", response.getBody().getError());
            assertEquals("Invalid config value format", response.getBody().getMessage());
        }
    }
}
