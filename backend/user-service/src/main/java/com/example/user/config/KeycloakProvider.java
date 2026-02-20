package com.example.user.config;

import lombok.Getter;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class KeycloakProvider {
    @Value("${keycloak.auth-server-url}")
    public String serverURL;
    @Value("${keycloak.realm}")
    public String realm;
    @Value("${keycloak.resource}")
    public String clientID;
    @Value("${keycloak.credentials.secret}")
    public String clientSecret;
    @Value("${keycloak.admin-client-id}")
    private String adminClientID;
    @Value("${keycloak.admin-client-secret}")
    private String adminClientSecret;

    public KeycloakProvider() {
    }

    private Keycloak adminInstance = null;
    private Keycloak loginInstance = null;

    public Keycloak getAdminInstance() {
        if (adminInstance == null) {
            adminInstance = KeycloakBuilder.builder()
                    .serverUrl(serverURL)
                    .realm(realm)
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .clientId(adminClientID)
                    .clientSecret(adminClientSecret)
                    .build();
        }
        return adminInstance;
    }

    public Keycloak getLoginInstance() {
        if (loginInstance == null) {
            loginInstance = KeycloakBuilder.builder()
                    .serverUrl(serverURL)
                    .realm(realm)
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .clientId(clientID)
                    .clientSecret(clientSecret)
                    .build();
        }
        return loginInstance;
    }
}