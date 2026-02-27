package org.arghyam.jalsoochak.scheme.controller;

import org.arghyam.jalsoochak.scheme.dto.SampleDTO;
import org.arghyam.jalsoochak.scheme.kafka.KafkaProducer;
import org.arghyam.jalsoochak.scheme.service.BusinessService;
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

    @GetMapping("/schemes")
    public ResponseEntity<List<SampleDTO>> getAllSchemes() {
        log.info("GET /api/schemes called");
        return ResponseEntity.ok(businessService.getAllSchemes());
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publishMessage(@RequestBody String message) {
        log.info("POST /api/publish called with message: {}", message);
        kafkaProducer.sendMessage(message);
        return ResponseEntity.ok("Message published to scheme-service-topic");
    }
}
