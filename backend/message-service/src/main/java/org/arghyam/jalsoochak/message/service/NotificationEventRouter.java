package org.arghyam.jalsoochak.message.service;

import org.arghyam.jalsoochak.message.channel.WhatsAppChannel;
import org.arghyam.jalsoochak.message.dto.OperatorEscalationDetail;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Routes incoming Kafka JSON messages to the appropriate notification handler
 * based on the {@code eventType} field.
 *
 * <ul>
 *   <li>{@code NUDGE} — fetches the localized message from tenant config and
 *       sends it as a WhatsApp HSM to the operator.</li>
 *   <li>{@code ESCALATION} — generates a PDF, uploads it to MinIO, fetches
 *       the localized body text, and sends a document HSM to the officer.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventRouter {

    private final ObjectMapper objectMapper;
    private final WhatsAppChannel whatsAppChannel;
    private final EscalationPdfService escalationPdfService;
    private final MinioStorageService minioStorageService;
    private final MessageTemplateService messageTemplateService;

    @Value("${escalation.report.dir:/tmp/escalation-reports/}")
    private String reportDir;

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

    //TODO: Verify each finding against the current code and only fix it if needed.
    //
    //In
    //`@backend/message-service/src/main/java/org/arghyam/jalsoochak/message/service/NotificationEventRouter.java`
    //around lines 84 - 98, handleNudge currently ignores tenant/language context and
    //sends a hardcoded template; extract tenantId, languageId (and schemeId if
    //needed) from the JsonNode (similar to handleEscalation) and use
    //messageTemplateService to resolve the tenant/language-specific nudge
    //template/message (e.g. call a new or existing
    //messageTemplateService.findNudgeMessage(tenantId, languageId) or reuse the
    //escalation lookup pattern), then pass the resolved template/message into
    //whatsAppChannel.sendNudge (or update sendNudge signature to accept the template)
    //so the nudge is localized per tenant and language.
    private void handleNudge(JsonNode root) {
        String phone = root.path("recipientPhone").asText("");
        String operatorName = root.path("operatorName").asText("Operator");

        if (phone.isBlank()) {
            log.warn("[Router/NUDGE] recipientPhone is blank, skipping");
            return;
        }

        String todayDate = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        boolean sent = whatsAppChannel.sendNudge(phone, operatorName, todayDate);
        if (!sent) {
                throw new IllegalStateException("[Router/NUDGE] WhatsApp nudge delivery failed");
            }
        log.info("[Router/NUDGE] → SENT");
        log.debug("[Router/NUDGE] phone={} → SENT", phone);
    }

    private void handleEscalation(JsonNode root) throws Exception {
        String officerPhone = root.path("officerPhone").asText("");
        String officerName = root.path("officerName").asText("Officer");
        int level = root.path("escalationLevel").asInt(1);
        int tenantId = root.path("tenantId").asInt(0);
        int officerLanguageId = root.path("officerLanguageId").asInt(0);

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
            log.warn("[Router/ESCALATION] No operators in event, skipping");
            return;
        }

        String filename = escalationPdfService.generate(operators, level, officerName);
        java.nio.file.Path localPath = Paths.get(reportDir, filename);
        String minioUrl;
        try {
            minioUrl = minioStorageService.upload(localPath);
        } finally {
            try {
                Files.deleteIfExists(localPath);
            } catch (Exception cleanupEx) {
                log.warn("[Router/ESCALATION] Could not delete local PDF {}: {}",
                                localPath, cleanupEx.getMessage());
            }
        }

        boolean sent = whatsAppChannel.sendDocument(officerPhone, minioUrl);
        if (!sent) {
            throw new IllegalStateException("[Router/ESCALATION] WhatsApp escalation delivery failed");
        }
        String loggableUrl = minioUrl.replaceFirst("\\?.*$", "");
        log.info("[Router/ESCALATION] level={} → {} ({})", level, sent ? "SENT" : "FAILED", loggableUrl);
        log.debug("[Router/ESCALATION] officer={} level={} → {} ({})", officerPhone, level,
                sent ? "SENT" : "FAILED", loggableUrl);
    }
}
