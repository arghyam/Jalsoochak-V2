package com.example.message.controller;

import com.example.message.dto.NotificationRequest;
import com.example.message.dto.SampleDTO;
import com.example.message.kafka.KafkaProducer;
import com.example.message.service.BusinessService;
import com.example.message.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final BusinessService businessService;
    private final NotificationService notificationService;
    private final KafkaProducer kafkaProducer;

    // ── GET all notifications (hardcoded) ─────────────────────

    @GetMapping("/notifications")
    public ResponseEntity<List<SampleDTO>> getAllNotifications() {
        log.info("GET /api/notifications called");
        return ResponseEntity.ok(businessService.getAllNotifications());
    }

    // ── POST send a notification via the specified channel ────

    @PostMapping("/notifications/send")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationRequest request) {
        log.info("POST /api/notifications/send called – channel={}, recipient={}",
                request.getChannel(), request.getRecipient());
        String result = notificationService.send(request);
        return ResponseEntity.ok(result);
    }

    // ── POST publish Kafka message ────────────────────────────

    @PostMapping("/publish")
    public ResponseEntity<String> publishMessage(@RequestBody String message) {
        log.info("POST /api/publish called with message: {}", message);
        kafkaProducer.sendMessage(message);
        return ResponseEntity.ok("Message published to message-service-topic");
    }
}
