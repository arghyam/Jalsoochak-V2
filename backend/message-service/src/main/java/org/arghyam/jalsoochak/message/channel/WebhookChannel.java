package org.arghyam.jalsoochak.message.channel;

import org.arghyam.jalsoochak.message.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Webhook (Push Notification) channel.
 * <p>
 * Sends an HTTP POST to the configured webhook URL (or the recipient URL)
 * with the notification payload as JSON.
 * <p>
 * Configure the default webhook endpoint and optional secret in application.yml:
 * <pre>
 *   notification.channel.webhook.url=YOUR_WEBHOOK_URL
 *   notification.channel.webhook.secret=YOUR_WEBHOOK_SECRET
 * </pre>
 */
@Component
@Slf4j
public class WebhookChannel implements NotificationChannel {

    private final WebClient webClient;

    @Value("${notification.channel.webhook.url:}")
    private String defaultWebhookUrl;

    @Value("${notification.channel.webhook.secret:}")
    private String webhookSecret;

    public WebhookChannel(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public String channelType() {
        return "WEBHOOK";
    }

    @Override
    public boolean send(NotificationRequest request) {
        String targetUrl = (request.getRecipient() != null && !request.getRecipient().isBlank())
                ? request.getRecipient()
                : defaultWebhookUrl;

        if (targetUrl == null || targetUrl.isBlank()) {
            log.warn("[WEBHOOK] No webhook URL configured and none provided in request. Skipping.");
            return false;
        }

        try {
            log.info("[WEBHOOK] Sending push notification to {}", targetUrl);

            Map<String, String> payload = Map.of(
                    "subject", request.getSubject() != null ? request.getSubject() : "",
                    "body", request.getBody() != null ? request.getBody() : ""
            );

            webClient.post()
                    .uri(targetUrl)
                    .header("X-Webhook-Secret", webhookSecret)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("[WEBHOOK] Push notification delivered successfully");
            return true;
        } catch (Exception ex) {
            log.error("[WEBHOOK] Failed to deliver push notification: {}", ex.getMessage(), ex);
            return false;
        }
    }
}
