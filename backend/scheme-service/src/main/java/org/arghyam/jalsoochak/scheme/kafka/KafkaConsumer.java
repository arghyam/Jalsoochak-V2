package org.arghyam.jalsoochak.scheme.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaConsumer {

    @KafkaListener(topics = "common-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        log.info("[scheme-service] Received message from common-topic: {}", message);
    }
}
