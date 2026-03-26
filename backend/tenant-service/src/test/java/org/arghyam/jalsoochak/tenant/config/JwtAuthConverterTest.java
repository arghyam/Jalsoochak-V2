package org.arghyam.jalsoochak.tenant.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthConverterTest {

    private static final String CLIENT_ID = "tenant-service";

    private final JwtAuthConverter converter = new JwtAuthConverter(CLIENT_ID);

    private static Jwt buildJwt(Map<String, Object> claims) {
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                claims);
    }

    @Test
    void convert_allClaims_producesAllAuthorities() {
        Jwt jwt = buildJwt(Map.of(
                "sub", "user-uuid",
                "preferred_username", "admin@example.com",
                "tenant_state_code", "mp",
                "user_type", "super_user",
                "realm_access", Map.of("roles", List.of("SUPER_USER")),
                "resource_access", Map.of(CLIENT_ID, Map.of("roles", List.of("STATE_ADMIN")))));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);
        Set<String> authorities = authorities(token);

        assertThat(token.getName()).isEqualTo("admin@example.com");
        assertThat(authorities).contains(
                "ROLE_SUPER_USER",
                "ROLE_STATE_ADMIN",
                "TENANT_MP",
                "USER_TYPE_SUPER_USER");
    }

    @Test
    void convert_tenantStateCodeIsUppercased() {
        Jwt jwt = buildJwt(Map.of(
                "sub", "user-uuid",
                "tenant_state_code", "tr"));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authorities(token)).contains("TENANT_TR");
    }

    @Test
    void convert_userTypeIsUppercased() {
        Jwt jwt = buildJwt(Map.of(
                "sub", "user-uuid",
                "user_type", "state_admin"));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authorities(token)).contains("USER_TYPE_STATE_ADMIN");
    }

    @Test
    void convert_missingPreferredUsername_fallsBackToSub() {
        Jwt jwt = buildJwt(Map.of("sub", "fallback-uuid"));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("fallback-uuid");
    }

    @Test
    void convert_missingRealmAccess_producesNoRealmRoles() {
        Jwt jwt = buildJwt(Map.of("sub", "user-uuid"));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authorities(token))
                .noneMatch(a -> a.startsWith("ROLE_"));
    }

    @Test
    void convert_resourceAccessForDifferentClient_producesNoClientRoles() {
        Jwt jwt = buildJwt(Map.of(
                "sub", "user-uuid",
                "resource_access", Map.of("other-client", Map.of("roles", List.of("SOME_ROLE")))));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authorities(token)).doesNotContain("ROLE_SOME_ROLE");
    }

    @Test
    void convert_blankClientId_producesNoClientRoles() {
        JwtAuthConverter converterNoClient = new JwtAuthConverter("");
        Jwt jwt = buildJwt(Map.of(
                "sub", "user-uuid",
                "resource_access", Map.of(CLIENT_ID, Map.of("roles", List.of("STATE_ADMIN")))));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converterNoClient.convert(jwt);

        assertThat(authorities(token)).doesNotContain("ROLE_STATE_ADMIN");
    }

    @Test
    void convert_missingTenantStateCode_producesNoTenantAuthority() {
        Jwt jwt = buildJwt(Map.of("sub", "user-uuid"));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authorities(token)).noneMatch(a -> a.startsWith("TENANT_"));
    }

    @Test
    void convert_missingUserType_producesNoUserTypeAuthority() {
        Jwt jwt = buildJwt(Map.of("sub", "user-uuid"));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authorities(token)).noneMatch(a -> a.startsWith("USER_TYPE_"));
    }

    private static Set<String> authorities(JwtAuthenticationToken token) {
        return token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
