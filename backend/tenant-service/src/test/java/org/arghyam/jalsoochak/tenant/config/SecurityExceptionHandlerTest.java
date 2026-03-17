package org.arghyam.jalsoochak.tenant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityExceptionHandler Tests")
class SecurityExceptionHandlerTest {

    private SecurityExceptionHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new SecurityExceptionHandler(new ObjectMapper());
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("commence should return 401 with WWW-Authenticate Bearer realm header")
    void commence_ShouldReturn401WithBearerRealmHeader() throws Exception {
        handler.commence(request, response, new AuthenticationException("unauthenticated") {});

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals("Bearer realm=\"jalsoochak\"", response.getHeader("WWW-Authenticate"));
        String body = response.getContentAsString();
        assertTrue(body.contains("401"));
        assertTrue(body.contains("Authentication required"));
    }

    @Test
    @DisplayName("handle should return 403 with WWW-Authenticate insufficient_scope header")
    void handle_ShouldReturn403WithInsufficientScopeHeader() throws Exception {
        handler.handle(request, response, new AccessDeniedException("forbidden"));

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        String wwwAuth = response.getHeader("WWW-Authenticate");
        assertNotNull(wwwAuth);
        assertTrue(wwwAuth.contains("Bearer"));
        assertTrue(wwwAuth.contains("insufficient_scope"));
        String body = response.getContentAsString();
        assertTrue(body.contains("403"));
        assertTrue(body.contains("Access denied"));
    }
}
