package org.arghyam.jalsoochak.tenant.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.tenant.repository.NudgeRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final ObjectMapper objectMapper;
    private final NudgeRepository nudgeRepository;

    @KafkaListener(topics = "common-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText("");

            if ("WHATSAPP_CONTACT_REGISTERED".equals(eventType)) {
                String schema = root.path("tenantSchema").asText("");
                long userId = root.path("userId").asLong(0);
                long contactId = root.path("contactId").asLong(0);
                if (!schema.isBlank() && userId > 0 && contactId > 0) {
                    int updated = nudgeRepository.updateWhatsAppConnectionId(schema, userId, contactId);
                    if (updated > 0) {
                            log.info("[tenant-service] Updated whatsapp_connection_id for userId={}", userId);
                        } else {
                            log.warn("[tenant-service] No rows updated for userId={} in schema={}", userId, schema);
                        }
                } else {
                    log.warn("[tenant-service] WHATSAPP_CONTACT_REGISTERED missing required fields, skipping");
                }
            } else {
                log.info("[tenant-service] Received message from common-topic, eventType={}", eventType);
            }
        } catch (Exception e) {
            log.error("[tenant-service] Failed to handle Kafka message: {}", e.getMessage(), e);
            // best-effort — acknowledge anyway to avoid redelivery loops;
            // failed contact ID updates will result in nudges using phone number instead
        }
        ack.acknowledge();
    }
}
