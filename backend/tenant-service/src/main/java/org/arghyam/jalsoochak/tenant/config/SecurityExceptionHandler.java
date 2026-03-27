package org.arghyam.jalsoochak.tenant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.tenant.dto.common.ApiErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer realm=\"jalsoochak\"");
        writeError(response, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setHeader("WWW-Authenticate",
                "Bearer realm=\"jalsoochak\", error=\"insufficient_scope\", error_description=\"Insufficient scope for this resource\"");
        writeError(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponseDTO body = new ApiErrorResponseDTO(status.value(), status.getReasonPhrase(), message, null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
