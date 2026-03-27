package org.arghyam.jalsoochak.anomaly.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.anomaly.dto.event.AnomalyEvent;
import org.arghyam.jalsoochak.anomaly.service.AnomalyIngestService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final ObjectMapper objectMapper;
    private final AnomalyIngestService anomalyIngestService;

    @KafkaListener(topics = "common-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        log.info("[anomaly-service] Received message from common-topic: {}", message);
    }

    @KafkaListener(topics = "telemetry-service-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTelemetryEvents(String message) {
        log.info("[anomaly-service] Received message from telemetry-service-topic");
        System.out.println("[anomaly-service] telemetry-service-topic raw=" + message);
        try {
            String eventType = extractEventType(message);
            if ("ANOMALY_RECORDED".equals(eventType)) {
                AnomalyEvent event = objectMapper.readValue(message, AnomalyEvent.class);
                System.out.println("[anomaly-service] ANOMALY_RECORDED uuid=" + event.getUuid()
                        + " type=" + event.getType()
                        + " tenantId=" + event.getTenantId()
                        + " schemeId=" + event.getSchemeId()
                        + " userId=" + event.getUserId());
                anomalyIngestService.ingest(event);
            } else {
                log.debug("[anomaly-service] Ignoring telemetry event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process telemetry event: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String extractEventType(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode eventTypeNode = node.get("eventType");
            return eventTypeNode != null ? eventTypeNode.asText() : "UNKNOWN";
        } catch (Exception e) {
            log.warn("Could not extract eventType from message, treating as UNKNOWN");
            return "UNKNOWN";
        }
    }
}
