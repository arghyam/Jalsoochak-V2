package org.arghyam.jalsoochak.message.service;

import org.arghyam.jalsoochak.message.channel.GlificWhatsAppService;
import org.arghyam.jalsoochak.message.channel.WhatsAppChannel;
import org.arghyam.jalsoochak.message.dto.OperatorEscalationDetail;
import org.arghyam.jalsoochak.message.event.WhatsAppContactRegisteredEvent;
import org.arghyam.jalsoochak.message.kafka.KafkaProducer;
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
 *   <li>{@code STAFF_SYNC_COMPLETED} — onboards pump operators into Glific and
 *       publishes {@code WHATSAPP_CONTACT_REGISTERED} events so tenant-service
 *       can persist the contact IDs.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventRouter {

    private static final String COMMON_TOPIC = "common-topic";

    private final ObjectMapper objectMapper;
    private final WhatsAppChannel whatsAppChannel;
    private final GlificWhatsAppService glificWhatsAppService;
    private final KafkaProducer kafkaProducer;
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
                case "STAFF_SYNC_COMPLETED" -> handleStaffSyncCompleted(root);
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
        String tenantSchema = root.path("tenantSchema").asText("");
        long userId = root.path("userId").asLong(0);
        long storedId = root.path("whatsappConnectionId").asLong(0);

        if (storedId <= 0 && phone.isBlank()) {
            log.warn("[Router/NUDGE] recipientPhone and whatsappConnectionId are both missing, skipping");
            return;
        }

        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        long contactId;
        if (storedId > 0) {
            contactId = storedId;
        } else {
            contactId = glificWhatsAppService.optIn(phone);
            if (!tenantSchema.isBlank() && userId > 0) {
                kafkaProducer.publishJson(COMMON_TOPIC,
                        WhatsAppContactRegisteredEvent.builder()
                                .eventType("WHATSAPP_CONTACT_REGISTERED")
                                .tenantSchema(tenantSchema)
                                .userId(userId)
                                .contactId(contactId)
                                .build());
            }
        }

        boolean sent = whatsAppChannel.sendNudgeViaFlow(contactId, operatorName, todayDate);
        if (!sent) {
            throw new IllegalStateException("[Router/NUDGE] WhatsApp nudge flow initiation failed");
        }
        log.info("[Router/NUDGE] → FLOW INITIATED");
        log.debug("[Router/NUDGE] phone={} → FLOW INITIATED", phone);
    }

    private void handleStaffSyncCompleted(JsonNode root) {
        JsonNode operatorsNode = root.path("pumpOperators");
        int glificLanguageId = root.path("glificLanguageId").asInt(0);
        String tenantSchema = root.path("tenantSchema").asText("");

        if (!operatorsNode.isArray() || operatorsNode.isEmpty()) {
            log.warn("[Router/STAFF_SYNC] pumpOperators is empty, skipping");
            return;
        }
        if (glificLanguageId == 0) {
            log.warn("[Router/STAFF_SYNC] glificLanguageId missing or zero, skipping");
            return;
        }

        int success = 0, failed = 0;
        for (JsonNode opNode : operatorsNode) {
            String phone = opNode.path("phone").asText("");
            long userId = opNode.path("userId").asLong(0);
            if (phone.isBlank()) {
                log.error("[Router/STAFF_SYNC] Operator userId={} has blank phone, skipping", userId);
                failed++;
                continue;
            }
            try {
                long contactId = whatsAppChannel.onboardOperator(phone, glificLanguageId);
                if (!tenantSchema.isBlank() && userId > 0) {
                    kafkaProducer.publishJson(COMMON_TOPIC,
                            WhatsAppContactRegisteredEvent.builder()
                                    .eventType("WHATSAPP_CONTACT_REGISTERED")
                                    .tenantSchema(tenantSchema)
                                    .userId(userId)
                                    .contactId(contactId)
                                    .build());
                    success++;
                }
            } catch (Exception e) {
                failed++;
                log.error("[Router/STAFF_SYNC] Failed to onboard operator: {}", e.getMessage(), e);
            }
        }
        log.info("[Router/STAFF_SYNC] Onboarding complete — success={} failed={} tenantSchema={}",
                success, failed, tenantSchema);
        if (failed > 0) {
            log.warn("[Router/STAFF_SYNC] {} operator onboarding(s) failed (success={}, tenantSchema={})",
                    failed, success, tenantSchema);
        }
    }

    private void handleEscalation(JsonNode root) throws Exception {
        String officerPhone = root.path("officerPhone").asText("");
        String officerName = root.path("officerName").asText("Officer");
        int level = root.path("escalationLevel").asInt(1);
        int tenantId = root.path("tenantId").asInt(0);
        int officerLanguageId = root.path("officerLanguageId").asInt(0);
        String tenantSchema = root.path("tenantSchema").asText("");
        long officerId = root.path("officerId").asLong(0);
        long storedId = root.path("officerWhatsappConnectionId").asLong(0);

        if (storedId <= 0 && officerPhone.isBlank()) {
            log.warn("[Router/ESCALATION] officerPhone and officerWhatsappConnectionId are both missing, skipping");
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

        long contactId;
        if (storedId > 0) {
            contactId = storedId;
        } else {
            contactId = glificWhatsAppService.optIn(officerPhone);
            if (!tenantSchema.isBlank() && officerId > 0) {
                kafkaProducer.publishJson(COMMON_TOPIC,
                        WhatsAppContactRegisteredEvent.builder()
                                .eventType("WHATSAPP_CONTACT_REGISTERED")
                                .tenantSchema(tenantSchema)
                                .userId(officerId)
                                .contactId(contactId)
                                .build());
            }
        }

        boolean sent = whatsAppChannel.sendDocument(contactId, minioUrl);
        if (!sent) {
            throw new IllegalStateException("[Router/ESCALATION] WhatsApp escalation delivery failed");
        }
        String loggableUrl = minioUrl.replaceFirst("\\?.*$", "");
        log.info("[Router/ESCALATION] level={} → {} ({})", level, sent ? "SENT" : "FAILED", loggableUrl);
        log.debug("[Router/ESCALATION] officer={} level={} → {} ({})", officerPhone, level,
                sent ? "SENT" : "FAILED", loggableUrl);
    }
}
