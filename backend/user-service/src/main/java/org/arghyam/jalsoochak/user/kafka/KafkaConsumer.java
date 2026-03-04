package org.arghyam.jalsoochak.user.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Placeholder Kafka consumer for user-service.
 * user-service does not consume from common-topic — NUDGE/ESCALATION events
 * are handled exclusively by message-service.
 */
@Component
@Slf4j
public class KafkaConsumer {

    @KafkaListener(topics = "common-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        log.info("[user-service] Received message from common-topic: {}", message);
    }
}
