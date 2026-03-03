package com.example.user.kafka;

import org.springframework.stereotype.Component;

/**
 * Placeholder Kafka consumer for user-service.
 * user-service does not consume from common-topic — NUDGE/ESCALATION events
 * are handled exclusively by message-service.
 */
@Component
public class KafkaConsumer {
}
