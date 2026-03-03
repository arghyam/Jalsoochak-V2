package org.arghyam.jalsoochak.message.channel;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Manages Glific session authentication.
 * Logs in on startup via POST /api/v1/session and can refresh tokens via PUT /api/v1/session/renew.
 */
@Service
@Slf4j
public class GlificAuthService {

    @Value("${glific.auth-url:https://api.arghyam.glific.com/api/v1/session}")
    private String authUrl;

    @Value("${glific.username:}")
    private String username;

    @Value("${glific.password:}")
    private String password;

    private final WebClient webClient;

    private volatile String accessToken;
    private volatile String renewalToken;

    public GlificAuthService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @PostConstruct
    public void login() {
        log.info("[GlificAuth] Logging in to Glific...");
        JsonNode data = webClient.post()
                .uri(authUrl)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of("user", Map.of("phone", username, "password", password)))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(30));

        if (data == null || !data.has("data")) {
            throw new RuntimeException("[GlificAuth] Login failed: null or unexpected response");
        }
        accessToken = data.path("data").path("access_token").asText();
        renewalToken = data.path("data").path("renewal_token").asText();
        log.info("[GlificAuth] Login successful, access token acquired");
    }

    public String getAccessToken() {
        return accessToken;
    }

    /** Refreshes tokens using the renewal token (PUT /api/v1/session/renew). */
    public synchronized void refresh() {
        log.info("[GlificAuth] Refreshing access token...");
        String renewUrl = authUrl + "/renew";
        JsonNode data = webClient.put()
                .uri(renewUrl)
                .header("Authorization", renewalToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(30));

        if (data == null || !data.has("data")) {
            throw new RuntimeException("[GlificAuth] Token refresh failed");
        }
        accessToken = data.path("data").path("access_token").asText();
        renewalToken = data.path("data").path("renewal_token").asText();
        log.info("[GlificAuth] Token refreshed successfully");
    }
}
