package com.example.anomaly.controller;

import com.example.anomaly.dto.SampleDTO;
import com.example.anomaly.kafka.KafkaProducer;
import com.example.anomaly.service.BusinessService;
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
    private final KafkaProducer kafkaProducer;

    @GetMapping("/anomalies")
    public ResponseEntity<List<SampleDTO>> getAllAnomalies() {
        log.info("GET /api/anomalies called");
        return ResponseEntity.ok(businessService.getAllAnomalies());
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publishMessage(@RequestBody String message) {
        log.info("POST /api/publish called with message: {}", message);
        kafkaProducer.sendMessage(message);
        return ResponseEntity.ok("Message published to anomaly-service-topic");
    }
}
