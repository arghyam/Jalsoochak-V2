package org.arghyam.jalsoochak.user.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@Component
public class KeycloakClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final String tokenUrl;
    private final String logoutUrl;
    private final String clientId;
    private final String clientSecret;

    public KeycloakClient(ObjectMapper objectMapper,
                          @Value("${keycloak.auth-server-url}") String authServerUrl,
                          @Value("${keycloak.realm}") String realm,
                          @Value("${keycloak.resource}") String clientId,
                          @Value("${keycloak.credentials.secret}") String clientSecret,
                          @Value("${http-client.connect-timeout-ms}") int connectTimeoutMs,
                          @Value("${http-client.read-timeout-ms}") int readTimeoutMs) {
        this.objectMapper = objectMapper;
        this.tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        this.logoutUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
        this.clientId = clientId;
        this.clientSecret = clientSecret;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public KeycloakTokenResponse obtainToken(String username, String password) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", username);
        body.add("password", password);
        body.add("grant_type", "password");
        body.add("scope", "openid");

        return postForToken(tokenUrl, body);
    }

    public KeycloakTokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        return postForToken(tokenUrl, body);
    }

    public boolean logout(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        try {
            restClient.post()
                    .uri(logoutUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Logout failed", e);
            return false;
        }
    }

    private KeycloakTokenResponse postForToken(String url, MultiValueMap<String, String> body) {
        try {
            String responseBody = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak token request failed");
            }

            return objectMapper.readValue(responseBody, KeycloakTokenResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Keycloak token error: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username, password, or refresh token", e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak token request failed", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Keycloak token error", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak token request failed", e);
        }
    }
}
