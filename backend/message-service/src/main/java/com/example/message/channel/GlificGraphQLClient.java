package com.example.message.channel;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Thin WebClient wrapper for the Glific GraphQL API.
 * POSTs {@code {"query": ..., "variables": ...}} with {@code Authorization: <token>} header
 * obtained from {@link GlificAuthService} (no Bearer prefix). On an unauthenticated error,
 * refreshes the token and retries once.
 */
@Component
@Slf4j
public class GlificGraphQLClient {

    private final WebClient webClient;
    private final GlificAuthService glificAuthService;

    @Value("${glific.api-url:}")
    private String apiUrl;

    public GlificGraphQLClient(WebClient.Builder builder, GlificAuthService glificAuthService) {
        this.webClient = builder.build();
        this.glificAuthService = glificAuthService;
    }

    public JsonNode execute(String query, Map<String, Object> variables) {
        return executeWithRetry(query, variables, false);
    }

    private JsonNode executeWithRetry(String query, Map<String, Object> variables, boolean isRetry) {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new RuntimeException("Glific API URL is not configured (glific.api-url)");
        }

        JsonNode response = webClient.post()
                .uri(apiUrl)
                .header("Authorization", glificAuthService.getAccessToken())
                .header("Content-Type", "application/json")
                .bodyValue(Map.of("query", query, "variables", variables))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(30));

        if (response == null) {
            throw new RuntimeException("Null response from Glific GraphQL");
        }

        if (response.has("errors") && response.get("errors").size() > 0) {
            String msg = response.get("errors").get(0).path("message").asText("unknown");
            if (!isRetry && (msg.toLowerCase().contains("unauthenticated")
                    || msg.toLowerCase().contains("unauthorized"))) {
                glificAuthService.refresh();
                return executeWithRetry(query, variables, true);
            }
            throw new RuntimeException("Glific GraphQL error: " + msg);
        }
        return response.get("data");
    }
}
