package org.arghyam.jalsoochak.analytics.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthConverterTest {

    private final JwtAuthConverter converter = new JwtAuthConverter("analytics-service");

    @Test
    void convert_extractsUuidTenantUserTypeAndRoles() {
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "11111111-1111-1111-1111-111111111111",
                        "preferred_username", "user@example.com",
                        "tenant_state_code", "mp",
                        "user_type", "state_admin",
                        "realm_access", Map.of("roles", java.util.List.of("SUPER_USER")),
                        "resource_access", Map.of(
                                "analytics-service", Map.of("roles", java.util.List.of("STATE_ADMIN")))));

        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) converter.convert(jwt);

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authentication.getName()).isEqualTo("user@example.com");
        assertThat(authorities).contains(
                "ROLE_SUPER_USER",
                "ROLE_STATE_ADMIN",
                "TENANT_MP",
                "USER_TYPE_STATE_ADMIN",
                "UUID_11111111-1111-1111-1111-111111111111");
    }
}
