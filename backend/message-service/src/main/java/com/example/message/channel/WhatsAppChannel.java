package com.example.message.channel;

import com.example.message.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
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

    /**
     * Sends a document (PDF) via WhatsApp using the Glific document message type.
     *
     * @param toPhone     recipient WhatsApp phone number
     * @param documentUrl publicly reachable URL of the document
     * @param caption     caption shown below the document in the chat
     * @return {@code true} if the request was accepted by Glific
     */
    public boolean sendDocument(String toPhone, String documentUrl, String caption) {
        if (glificApiUrl == null || glificApiUrl.isBlank()) {
            log.warn("[WHATSAPP] Gliffic API URL not configured. Skipping document delivery.");
            return false;
        }

        try {
            log.info("[WHATSAPP] Sending document to {} via Gliffic: {}", toPhone, documentUrl);

            Map<String, Object> document = new HashMap<>();
            document.put("url", documentUrl);
            document.put("caption", caption != null ? caption : "");

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", fromNumber);
            payload.put("to", toPhone);
            payload.put("type", "document");
            payload.put("document", document);

            webClient.post()
                    .uri(glificApiUrl)
                    .header("Authorization", "Bearer " + glificApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("[WHATSAPP] Document delivered successfully to {}", toPhone);
            return true;
        } catch (Exception ex) {
            log.error("[WHATSAPP] Failed to deliver document to {}: {}", toPhone, ex.getMessage(), ex);
            return false;
        }
    }
}
