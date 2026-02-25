package com.example.message.service;

import com.example.message.channel.WhatsAppChannel;
import com.example.message.dto.NotificationRequest;
import com.example.message.dto.OperatorEscalationDetail;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes incoming Kafka JSON messages to the appropriate notification handler
 * based on the {@code eventType} field.
 *
 * <ul>
 *   <li>{@code NUDGE} — sends a WhatsApp text message to the operator.</li>
 *   <li>{@code ESCALATION} — generates a PDF and sends it as a WhatsApp document
 *       to the escalation officer.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventRouter {

    private final ObjectMapper objectMapper;
    private final WhatsAppChannel whatsAppChannel;
    private final EscalationPdfService escalationPdfService;

    @Value("${app.base-url:http://localhost:8085}")
    private String baseUrl;

    @PostConstruct
    void validateBaseUrl() {
        if (baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")) {
            log.warn("[Router] app.base-url is set to a local address ('{}')."
                    + " PDF links embedded in escalation WhatsApp messages will be unreachable by Glific."
                    + " Set the 'app.base-url' property to a publicly reachable URL"
                    + " (e.g., 'APP_BASE_URL=https://<id>.ngrok.io' for demos,"
                    + " or your server's public hostname in production) before sending escalation reports.",
                    baseUrl);
        }
    }

    /**
     * Routes the message and re-throws any processing exception so the Kafka
     * container's error handler can apply its retry/back-off policy and, if
     * configured, forward the payload to a dead-letter topic.
     *
     * <p>Unknown {@code eventType} values are silently skipped — they are
     * permanent non-retryable conditions and must not cause infinite retries.</p>
     */
    public void route(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String eventType = root.path("eventType").asText("");

            switch (eventType.toUpperCase()) {
                case "NUDGE" -> handleNudge(root);
                case "ESCALATION" -> handleEscalation(root);
                default -> log.warn("[Router] Unknown eventType '{}', ignoring message", eventType);
            }
        } catch (Exception e) {
            log.error("[Router] Failed to process Kafka message, rethrowing for container retry/DLT: {}",
                    e.getMessage(), e);
            throw new RuntimeException("Notification event processing failed", e);
        }
    }

    private void handleNudge(JsonNode root) {
        String phone = root.path("recipientPhone").asText("");
        String operatorName = root.path("operatorName").asText("Operator");
        String schemeId = root.path("schemeId").asText("");

        if (phone.isBlank()) {
            log.warn("[Router/NUDGE] recipientPhone is blank, skipping");
            return;
        }

        String text = String.format(
                "Dear %s, please submit your daily water reading for scheme %s. Thank you.",
                operatorName, schemeId);

        NotificationRequest request = NotificationRequest.builder()
                .recipient(phone)
                .body(text)
                .channel("WHATSAPP")
                .build();

        boolean sent = whatsAppChannel.send(request);
        log.info("[Router/NUDGE] phone={} → {}", phone, sent ? "SENT" : "FAILED");
    }

    private void handleEscalation(JsonNode root) throws Exception {
        String officerPhone = root.path("officerPhone").asText("");
        String officerName = root.path("officerName").asText("Officer");
        int level = root.path("escalationLevel").asInt(1);

        if (officerPhone.isBlank()) {
            log.warn("[Router/ESCALATION] officerPhone is blank, skipping");
            return;
        }

        JsonNode operatorsNode = root.path("operators");
        List<OperatorEscalationDetail> operators = new ArrayList<>();
        if (operatorsNode.isArray()) {
            for (JsonNode node : operatorsNode) {
                operators.add(objectMapper.treeToValue(node, OperatorEscalationDetail.class));
            }
        }

        if (operators.isEmpty()) {
            log.warn("[Router/ESCALATION] No operators in event for officer={}, skipping", officerPhone);
            return;
        }

        String filename = escalationPdfService.generate(operators, level, officerName);
        String pdfUrl = baseUrl + "/api/v1/reports/" + filename;

        String caption = String.format("Escalation Report – Level %d – %s", level, officerName);
        boolean sent = whatsAppChannel.sendDocument(officerPhone, pdfUrl, caption);
        log.info("[Router/ESCALATION] officer={} level={} → {} ({})", officerPhone, level,
                sent ? "SENT" : "FAILED", pdfUrl);
    }
}
