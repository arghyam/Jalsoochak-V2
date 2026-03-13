package org.arghyam.jalsoochak.scheme.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.scheme.dto.SchemeDTO;
import org.arghyam.jalsoochak.scheme.dto.SchemeUploadResponseDTO;
import org.arghyam.jalsoochak.scheme.kafka.KafkaProducer;
import org.arghyam.jalsoochak.scheme.service.SchemeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SchemeController {

    private final SchemeService schemeService;
    private final KafkaProducer kafkaProducer;

    @GetMapping("/schemes")
    public ResponseEntity<List<SchemeDTO>> getAllSchemes() {
        log.info("GET /api/schemes called");
        return ResponseEntity.ok(schemeService.getAllSchemes());
    }

    @PostMapping(value = "/schemes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SchemeUploadResponseDTO> uploadSchemes(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("POST /api/schemes/upload called with file: {}", file.getOriginalFilename());
        return ResponseEntity.ok(schemeService.uploadSchemes(file, authorizationHeader));
    }

    @PostMapping(value = "/schemes/mappings/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SchemeUploadResponseDTO> uploadSchemeMappings(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("POST /api/schemes/mappings/upload called with file: {}", file.getOriginalFilename());
        return ResponseEntity.ok(schemeService.uploadSchemeMappings(file, authorizationHeader));
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publishMessage(@RequestBody String message) {
        log.info("POST /api/publish called with message: {}", message);
        kafkaProducer.sendMessage(message);
        return ResponseEntity.ok("Message published to scheme-service-topic");
    }
}
