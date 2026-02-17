package com.example.tenant.controller;

import com.example.tenant.dto.SampleDTO;
import com.example.tenant.kafka.KafkaProducer;
import com.example.tenant.service.BusinessService;
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

    @GetMapping("/tenants")
    public ResponseEntity<List<SampleDTO>> getAllTenants() {
        log.info("GET /api/tenants called");
        return ResponseEntity.ok(businessService.getAllTenants());
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publishMessage(@RequestBody String message) {
        log.info("POST /api/publish called with message: {}", message);
        kafkaProducer.sendMessage(message);
        return ResponseEntity.ok("Message published to tenant-service-topic");
    }
}
