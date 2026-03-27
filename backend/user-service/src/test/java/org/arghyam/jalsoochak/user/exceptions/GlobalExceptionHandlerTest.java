package org.arghyam.jalsoochak.user.exceptions;

import org.arghyam.jalsoochak.user.dto.common.ApiErrorResponseDTO;
import org.arghyam.jalsoochak.user.enums.AdminUserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleTypeMismatch_enumType_includesAcceptedValuesAndFallsBackToExName() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        MethodParameter param = mock(MethodParameter.class);

        when(ex.getRequiredType()).thenAnswer(inv -> AdminUserStatus.class);
        when(ex.getParameter()).thenReturn(param);
        when(param.getParameterName()).thenReturn(null); // triggers fallback to ex.getName()
        when(ex.getName()).thenReturn("status");
        when(ex.getValue()).thenReturn("WRONG");

        ResponseEntity<ApiErrorResponseDTO> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        String message = response.getBody().getMessage();
        assertThat(message).contains("WRONG");
        assertThat(message).contains("status");
        assertThat(message).contains("INACTIVE");
        assertThat(message).contains("ACTIVE");
        assertThat(message).contains("PENDING");
    }

    @Test
    void handleTypeMismatch_nullRequiredType_producesGenericMessage() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        MethodParameter param = mock(MethodParameter.class);

        when(ex.getRequiredType()).thenReturn(null);
        when(ex.getParameter()).thenReturn(param);
        when(param.getParameterName()).thenReturn("id");
        when(ex.getValue()).thenReturn("xyz");

        ResponseEntity<ApiErrorResponseDTO> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("xyz");
        assertThat(response.getBody().getMessage()).contains("id");
        assertThat(response.getBody().getMessage()).doesNotContain("Accepted values");
    }

    @Test
    void handleTypeMismatch_nonEnumType_producesGenericMessage() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        MethodParameter param = mock(MethodParameter.class);

        when(ex.getRequiredType()).thenAnswer(inv -> Integer.class);
        when(ex.getParameter()).thenReturn(param);
        when(param.getParameterName()).thenReturn("page");
        when(ex.getValue()).thenReturn("abc");

        ResponseEntity<ApiErrorResponseDTO> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String message = response.getBody().getMessage();
        assertThat(message).contains("abc");
        assertThat(message).contains("page");
        assertThat(message).doesNotContain("Accepted values");
    }
}
