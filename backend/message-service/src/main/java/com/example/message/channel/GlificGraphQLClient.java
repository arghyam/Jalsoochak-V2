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
 * POSTs {@code {"query": ..., "variables": ...}} with Bearer auth and
 * throws on any {@code errors[]} returned by Glific.
 */
@Component
@Slf4j
public class GlificGraphQLClient {

    private final WebClient webClient;

    @Value("${glific.api-url:}")
    private String apiUrl;

    @Value("${glific.api-key:}")
    private String apiKey;

    public GlificGraphQLClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public JsonNode execute(String query, Map<String, Object> variables) {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new RuntimeException("Glific API URL is not configured (glific.api-url)");
        }

        JsonNode response = webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of("query", query, "variables", variables))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(30));

        if (response == null) {
            throw new RuntimeException("Null response from Glific GraphQL");
        }
        if (response.has("errors") && response.get("errors").size() > 0) {
            throw new RuntimeException("Glific GraphQL error: "
                    + response.get("errors").get(0).path("message").asText("unknown"));
        }
        return response.get("data");
    }
}
