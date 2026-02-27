package org.arghyam.jalsoochak.tenant.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityUtils utility class.
 * Tests JWT extraction and security context operations.
 * Covers success scenarios, null handling, and edge cases.
 */
@DisplayName("Security Utils Tests")
class SecurityUtilsTest {

    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Create a fresh security context for each test
        securityContext = mock(SecurityContext.class);
        authentication = null;
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("Get Current User UUID")
    class GetCurrentUserUuidTests {

        @Test
        @DisplayName("Should extract UUID from JWT subject when authentication present")
        void testGetCurrentUserUuid_WithValidJwt() {
            // Arrange
            String expectedUuid = "user-uuid-12345";
            Jwt jwt = createMockJwt(expectedUuid, "John Doe");
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);

            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserUuid();

            // Assert
            assertEquals(expectedUuid, result);
        }

        @Test
        @DisplayName("Should return null when no authentication present")
        void testGetCurrentUserUuid_NoAuthentication() {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(null);

            // Act
            String result = SecurityUtils.getCurrentUserUuid();

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null when authentication principal is not JWT")
        void testGetCurrentUserUuid_PrincipalNotJwt() {
            // Arrange
            authentication = new UsernamePasswordAuthenticationToken("username", "password");
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserUuid();

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null when JWT subject is null")
        void testGetCurrentUserUuid_JwtSubjectNull() {
            // Arrange
            Jwt jwt = createMockJwtWithNullSubject();
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserUuid();

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle empty JWT subject")
        void testGetCurrentUserUuid_EmptyJwtSubject() {
            // Arrange
            Jwt jwt = createMockJwt("", "John Doe");
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserUuid();

            // Assert
            assertEquals("", result);
        }

        @Test
        @DisplayName("Should extract UUID from JWT with multiple claims")
        void testGetCurrentUserUuid_WithMultipleClaims() {
            // Arrange
            String expectedUuid = "multi-claim-uuid";
            Map<String, Object> claims = new HashMap<>();
            claims.put("name", "Jane Doe");
            claims.put("email", "jane@example.com");
            claims.put("roles", "ADMIN");

            Jwt jwt = createMockJwtWithClaims(expectedUuid, claims);
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserUuid();

            // Assert
            assertEquals(expectedUuid, result);
        }
    }

    @Nested
    @DisplayName("Get Current User Name")
    class GetCurrentUserNameTests {

        @Test
        @DisplayName("Should extract name claim from JWT when authentication present")
        void testGetCurrentUserName_WithValidJwt() {
            // Arrange
            String expectedName = "John Doe";
            Jwt jwt = createMockJwt("user-uuid", expectedName);
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserName();

            // Assert
            assertEquals(expectedName, result);
        }

        @Test
        @DisplayName("Should return 'System' when no authentication present")
        void testGetCurrentUserName_NoAuthentication() {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(null);

            // Act
            String result = SecurityUtils.getCurrentUserName();

            // Assert
            assertEquals("System", result);
        }

        @Test
        @DisplayName("Should return 'System' when authentication principal is not JWT")
        void testGetCurrentUserName_PrincipalNotJwt() {
            // Arrange
            authentication = new UsernamePasswordAuthenticationToken("username", "password");
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserName();

            // Assert
            assertEquals("System", result);
        }

        @Test
        @DisplayName("Should return 'System' when name claim is null")
        void testGetCurrentUserName_NameClaimNull() {
            // Arrange
            Jwt jwt = mock(Jwt.class);
            when(jwt.getClaimAsString("name")).thenReturn(null);
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserName();

            // Assert
            assertEquals("System", result);
        }

        @Test
        @DisplayName("Should handle empty name claim")
        void testGetCurrentUserName_EmptyNameClaim() {
            // Arrange
            Jwt jwt = createMockJwt("user-uuid", "");
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserName();

            // Assert
            assertEquals("", result);
        }

        @Test
        @DisplayName("Should extract name from JWT with special characters")
        void testGetCurrentUserName_WithSpecialCharacters() {
            // Arrange
            String nameWithSpecialChars = "José García-López";
            Jwt jwt = createMockJwt("user-uuid", nameWithSpecialChars);
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserName();

            // Assert
            assertEquals(nameWithSpecialChars, result);
        }

        @Test
        @DisplayName("Should extract name from JWT with spaces")
        void testGetCurrentUserName_WithSpaces() {
            // Arrange
            String nameWithSpaces = "   John Doe   ";
            Jwt jwt = createMockJwt("user-uuid", nameWithSpaces);
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserName();

            // Assert
            assertEquals(nameWithSpaces, result);
        }

        @Test
        @DisplayName("Should handle long names")
        void testGetCurrentUserName_LongName() {
            // Arrange
            String longName = "Alexander Michael Christopher David Edward Franklin George Henry".repeat(2);
            Jwt jwt = createMockJwt("user-uuid", longName);
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserName();

            // Assert
            assertEquals(longName, result);
        }
    }

    @Nested
    @DisplayName("JWT and Security Context Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle both UUID and name extraction in same flow")
        void testGetCurrentUserUuidAndName_Together() {
            // Arrange
            String uuid = "complete-uuid";
            String name = "Complete User";
            Jwt jwt = createMockJwt(uuid, name);
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String resultUuid = SecurityUtils.getCurrentUserUuid();
            String resultName = SecurityUtils.getCurrentUserName();

            // Assert
            assertEquals(uuid, resultUuid);
            assertEquals(name, resultName);
        }

        @Test
        @DisplayName("Should handle switching security context")
        void testSecurityContextSwitch() {
            // Arrange - First user
            Jwt jwt1 = createMockJwt("uuid-1", "User One");
            authentication = new UsernamePasswordAuthenticationToken(jwt1, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act & Assert - First user
            assertEquals("uuid-1", SecurityUtils.getCurrentUserUuid());
            assertEquals("User One", SecurityUtils.getCurrentUserName());

            // Arrange - Switch to second user
            SecurityContext newContext = mock(SecurityContext.class);
            Jwt jwt2 = createMockJwt("uuid-2", "User Two");
            Authentication newAuth = new UsernamePasswordAuthenticationToken(jwt2, null);
            when(newContext.getAuthentication()).thenReturn(newAuth);
            SecurityContextHolder.setContext(newContext);

            // Act & Assert - Second user
            assertEquals("uuid-2", SecurityUtils.getCurrentUserUuid());
            assertEquals("User Two", SecurityUtils.getCurrentUserName());
        }

        @Test
        @DisplayName("Should handle very long UUID")
        void testGetCurrentUserUuid_VeryLongUuid() {
            // Arrange
            String longUuid = "a".repeat(500);
            Jwt jwt = createMockJwt(longUuid, "User");
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserUuid();

            // Assert
            assertEquals(longUuid, result);
        }

        @Test
        @DisplayName("Should handle UUID with special characters")
        void testGetCurrentUserUuid_WithSpecialCharacters() {
            // Arrange
            String specialUuid = "uuid-!@#$%^&*()_+-=[]{}|;:,.<>?";
            Jwt jwt = createMockJwt(specialUuid, "User");
            authentication = new UsernamePasswordAuthenticationToken(jwt, null);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            // Act
            String result = SecurityUtils.getCurrentUserUuid();

            // Assert
            assertEquals(specialUuid, result);
        }
    }

    // Helper methods for creating mock JWT tokens

    /**
     * Creates a mock JWT token with subject and name claim.
     */
    private Jwt createMockJwt(String subject, String name) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(subject);
        when(jwt.getClaimAsString("name")).thenReturn(name);
        return jwt;
    }

    /**
     * Creates a mock JWT token with subject and custom claims.
     */
    private Jwt createMockJwtWithClaims(String subject, Map<String, Object> claims) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(subject);
        String name = (String) claims.getOrDefault("name", null);
        when(jwt.getClaimAsString("name")).thenReturn(name);
        return jwt;
    }

    /**
     * Creates a mock JWT token with null subject.
     */
    private Jwt createMockJwtWithNullSubject() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(null);
        when(jwt.getClaimAsString("name")).thenReturn("Test User");
        return jwt;
    }
}
