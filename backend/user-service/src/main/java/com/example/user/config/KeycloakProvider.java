package com.example.user.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakProvider {
    private final String serverURL;
    private final String realm;
    private final Keycloak adminInstance;
    private final Keycloak loginInstance;

    public KeycloakProvider(
            @Value("${keycloak.auth-server-url}") String serverURL,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.resource}") String clientID,
            @Value("${keycloak.credentials.secret}") String clientSecret,
            @Value("${keycloak.admin-client-id}") String adminClientID,
            @Value("${keycloak.admin-client-secret}") String adminClientSecret) {
        this.serverURL = serverURL;
        this.realm = realm;
        this.adminInstance = KeycloakBuilder.builder()
                .serverUrl(serverURL)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(adminClientID)
                .clientSecret(adminClientSecret)
                .build();
        this.loginInstance = KeycloakBuilder.builder()
                .serverUrl(serverURL)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientID)
                .clientSecret(clientSecret)
                .build();
    }

    public String getServerURL() {
        return serverURL;
    }

    public String getRealm() {
        return realm;
    }

    public Keycloak getAdminInstance() {
        return adminInstance;
    }

    public Keycloak getLoginInstance() {
        return loginInstance;
    }
}
