package org.arghyam.jalsoochak.user.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KeycloakClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final String tokenUrl;
    private final String logoutUrl;
    private final String clientId;
    private final String clientSecret;

    public KeycloakClient(RestTemplate restTemplate,
                          ObjectMapper objectMapper,
                          @Value("${keycloak.auth-server-url}") String authServerUrl,
                          @Value("${keycloak.realm}") String realm,
                          @Value("${keycloak.resource}") String clientId,
                          @Value("${keycloak.credentials.secret}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        this.logoutUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public Map<String, Object> obtainToken(String username, String password) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", username);
        body.add("password", password);
        body.add("grant_type", "password");
        body.add("scope", "openid");

        return postForToken(tokenUrl, body);
    }

    public Map<String, Object> refreshToken(String refreshToken) {
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
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers());
            ResponseEntity<String> response = restTemplate.postForEntity(logoutUrl, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Logout failed", e);
            return false;
        }
    }

    private Map<String, Object> postForToken(String url, MultiValueMap<String, String> body) {
        try {
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers());
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak token request failed");
            }

            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (RestClientResponseException e) {
            log.error("Keycloak token error: status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(e.getRawStatusCode());
            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.BAD_REQUEST) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username, password, or refresh token", e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak token request failed", e);
        } catch (Exception e) {
            log.error("Keycloak token error", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak token request failed", e);
        }
    }

    public Map<String, Object> getUserInfo(String accessToken) {
        String userInfoUrl = tokenUrl.replace("/token", "/userinfo"); // dynamically get userinfo endpoint
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(userInfoUrl,
                    org.springframework.http.HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak userinfo request failed");
            }

            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (RestClientResponseException e) {
            log.error("Keycloak userinfo error: status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(e.getRawStatusCode());
            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token is invalid or expired", e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak userinfo request failed", e);
        } catch (Exception e) {
            log.error("Keycloak userinfo error", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak userinfo request failed", e);
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
