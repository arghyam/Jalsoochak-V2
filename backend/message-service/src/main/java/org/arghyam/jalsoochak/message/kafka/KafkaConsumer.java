package org.arghyam.jalsoochak.message.kafka;

import org.arghyam.jalsoochak.message.service.NotificationEventRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final NotificationEventRouter notificationEventRouter;

    @KafkaListener(topics = "common-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        log.debug("[message-service] Received message from common-topic: {}", message);
        notificationEventRouter.route(message);
    }
}
