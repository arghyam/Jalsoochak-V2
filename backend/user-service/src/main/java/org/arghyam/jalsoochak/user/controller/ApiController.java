package org.arghyam.jalsoochak.user.controller;

import org.arghyam.jalsoochak.user.dto.SampleDTO;
import org.arghyam.jalsoochak.user.kafka.KafkaProducer;
import org.arghyam.jalsoochak.user.service.BusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<SampleDTO>> getAllUsers() {
        log.info("GET /api/users called");
        return ResponseEntity.ok(businessService.getAllUsers());
    }


    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/publish")
    public ResponseEntity<String> publishMessage(@RequestBody String message) {
        log.info("POST /api/publish called with message: {}", message);
        kafkaProducer.sendMessage(message);
        return ResponseEntity.ok("Message published to user-service-topic");
    }
}
