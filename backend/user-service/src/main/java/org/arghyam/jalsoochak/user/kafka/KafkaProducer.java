package org.arghyam.jalsoochak.user.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private static final String TOPIC = "user-service-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String message) {
        log.info("Publishing message to topic [{}]: {}", TOPIC, message);
        kafkaTemplate.send(TOPIC, message);
    }
}
