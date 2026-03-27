package org.arghyam.jalsoochak.analytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
    private final String keycloakClientId;

    public JwtAuthConverter(@Value("${keycloak.resource:}") String keycloakClientId) {
        this.keycloakClientId = keycloakClientId == null ? "" : keycloakClientId.trim();
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = Stream.of(
                        defaultConverter.convert(jwt),
                        extractRealmRoles(jwt),
                        extractClientRoles(jwt),
                        extractTenantAuthority(jwt),
                        extractUuidAuthority(jwt),
                        extractUserTypeAuthority(jwt))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        String principalName = jwt.getClaimAsString("preferred_username");
        if (principalName == null || principalName.isBlank()) {
            principalName = jwt.getSubject();
        }

        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return Collections.emptySet();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    private Collection<GrantedAuthority> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null || resourceAccess.isEmpty()) {
            return Collections.emptySet();
        }
        Object clientBlock = keycloakClientId.isBlank() ? null : resourceAccess.get(keycloakClientId);
        if (clientBlock instanceof Map<?, ?> clientMap) {
            Object rolesObj = clientMap.get("roles");
            if (rolesObj instanceof List<?> roles) {
                return roles.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }

    private Collection<GrantedAuthority> extractTenantAuthority(Jwt jwt) {
        String tenantCode = jwt.getClaimAsString("tenant_state_code");
        if (tenantCode == null || tenantCode.isBlank()) {
            return Collections.emptySet();
        }
        return Set.of(new SimpleGrantedAuthority("TENANT_" + tenantCode.toUpperCase()));
    }

    private Collection<GrantedAuthority> extractUuidAuthority(Jwt jwt) {
        String uuid = jwt.getSubject();
        if (uuid == null || uuid.isBlank()) {
            return Collections.emptySet();
        }
        return Set.of(new SimpleGrantedAuthority("UUID_" + uuid));
    }

    private Collection<GrantedAuthority> extractUserTypeAuthority(Jwt jwt) {
        String userType = jwt.getClaimAsString("user_type");
        if (userType == null || userType.isBlank()) {
            return Collections.emptySet();
        }
        return Set.of(new SimpleGrantedAuthority("USER_TYPE_" + userType.toUpperCase()));
    }
}
