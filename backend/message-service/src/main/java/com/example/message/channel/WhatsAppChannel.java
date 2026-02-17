package com.example.message.channel;

import com.example.message.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * WhatsApp channel powered by <strong>Gliffic</strong>.
 * <p>
 * Sends WhatsApp messages through the Gliffic messaging API.
 * <p>
 * Configure credentials in application.yml:
 * <pre>
 *   notification.channel.whatsapp.gliffic-api-url=YOUR_GLIFFIC_API_URL
 *   notification.channel.whatsapp.gliffic-api-key=YOUR_GLIFFIC_API_KEY
 *   notification.channel.whatsapp.from-number=YOUR_WHATSAPP_SENDER_NUMBER
 * </pre>
 */
@Component
@Slf4j
public class WhatsAppChannel implements NotificationChannel {

    private final WebClient webClient;

    @Value("${notification.channel.whatsapp.gliffic-api-url:}")
    private String glificApiUrl;

    @Value("${notification.channel.whatsapp.gliffic-api-key:}")
    private String glificApiKey;

    @Value("${notification.channel.whatsapp.from-number:}")
    private String fromNumber;

    public WhatsAppChannel(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public String channelType() {
        return "WHATSAPP";
    }

    @Override
    public boolean send(NotificationRequest request) {
        if (glificApiUrl == null || glificApiUrl.isBlank()) {
            log.warn("[WHATSAPP] Gliffic API URL not configured. Skipping WhatsApp delivery.");
            return false;
        }

        try {
            log.info("[WHATSAPP] Sending WhatsApp message via Gliffic to {}", request.getRecipient());

            Map<String, String> payload = Map.of(
                    "from", fromNumber,
                    "to", request.getRecipient(),
                    "body", request.getBody() != null ? request.getBody() : ""
            );

            webClient.post()
                    .uri(glificApiUrl)
                    .header("Authorization", "Bearer " + glificApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("[WHATSAPP] WhatsApp message delivered successfully via Gliffic");
            return true;
        } catch (Exception ex) {
            log.error("[WHATSAPP] Failed to deliver WhatsApp message: {}", ex.getMessage(), ex);
            return false;
        }
    }
}
