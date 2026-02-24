package com.example.message.controller;

import com.example.message.dto.NotificationRequest;
import com.example.message.dto.SampleDTO;
import com.example.message.kafka.KafkaProducer;
import com.example.message.service.BusinessService;
import com.example.message.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final BusinessService businessService;
    private final NotificationService notificationService;
    private final KafkaProducer kafkaProducer;

    @Value("${escalation.report.dir:/tmp/escalation-reports/}")
    private String reportDir;

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

    // ── GET escalation report PDF ─────────────────────────────
    // TODO: Verify each finding against the current code and only fix it if needed.
    //
    //In
    //`@backend/message-service/src/main/java/com/example/message/controller/ApiController.java`
    //around lines 69 - 88, The getReport method in ApiController currently serves
    //PDFs containing PII without requiring authentication; modify
    //ApiController.getReport to enforce access control by requiring an authenticated
    //user and appropriate role/permission (e.g., via Spring Security annotations like
    //`@PreAuthorize`("hasRole('REPORT_VIEWER')") or by checking SecurityContextHolder
    //inside getReport) before loading the FileSystemResource, and deny access with
    //401/403 if unauthorized; additionally, stop relying on guessable filenames by
    //generating and storing unpredictable filenames (e.g., include a UUID when saving
    //reports and keep a mapping from business id → stored filename) and update any
    //code paths that create/read files to use the UUID-backed filename (related
    //symbols: getReport, reportDir, filename) so only authorized users can fetch
    //reports and filenames cannot be guessed.

    @GetMapping("/v1/reports/{filename}")
    public ResponseEntity<Resource> getReport(@PathVariable String filename) {
        log.info("GET /api/v1/reports/{}", filename);

        // Sanitize filename to prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        Path filePath = Paths.get(reportDir, filename);
        FileSystemResource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Allow only safe characters in filename for header use
        String safeFilename = filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFilename + "\"")
                .body(resource);
    }
}
