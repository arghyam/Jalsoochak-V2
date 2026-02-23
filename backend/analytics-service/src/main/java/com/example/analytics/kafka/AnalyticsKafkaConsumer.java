package com.example.analytics.kafka;

import com.example.analytics.dto.event.DepartmentLocationEvent;
import com.example.analytics.dto.event.EscalationEvent;
import com.example.analytics.dto.event.LgdLocationEvent;
import com.example.analytics.dto.event.MeterReadingEvent;
import com.example.analytics.dto.event.SchemeEvent;
import com.example.analytics.dto.event.SchemePerformanceEvent;
import com.example.analytics.dto.event.TenantEvent;
import com.example.analytics.dto.event.UserEvent;
import com.example.analytics.dto.event.WaterQuantityEvent;
import com.example.analytics.service.DimensionService;
import com.example.analytics.service.FactService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final DimensionService dimensionService;
    private final FactService factService;

    @KafkaListener(topics = "tenant-service-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTenantEvents(String message) {
        log.info("[analytics] Received from tenant-service-topic");
        try {
            String eventType = extractEventType(message);
            switch (eventType) {
                case "TENANT_CREATED", "TENANT_UPDATED" -> {
                    TenantEvent event = objectMapper.readValue(message, TenantEvent.class);
                    dimensionService.upsertTenant(event);
                }
                default -> log.debug("Ignoring tenant event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process tenant event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "user-service-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUserEvents(String message) {
        log.info("[analytics] Received from user-service-topic");
        try {
            String eventType = extractEventType(message);
            switch (eventType) {
                case "USER_CREATED", "USER_UPDATED" -> {
                    UserEvent event = objectMapper.readValue(message, UserEvent.class);
                    dimensionService.upsertUser(event);
                }
                default -> log.debug("Ignoring user event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process user event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "scheme-service-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeSchemeEvents(String message) {
        log.info("[analytics] Received from scheme-service-topic");
        try {
            String eventType = extractEventType(message);
            switch (eventType) {
                case "SCHEME_CREATED", "SCHEME_UPDATED" -> {
                    SchemeEvent event = objectMapper.readValue(message, SchemeEvent.class);
                    dimensionService.upsertScheme(event);
                }
                case "LGD_LOCATION_CREATED", "LGD_LOCATION_UPDATED" -> {
                    LgdLocationEvent event = objectMapper.readValue(message, LgdLocationEvent.class);
                    dimensionService.upsertLgdLocation(event);
                }
                case "DEPARTMENT_LOCATION_CREATED", "DEPARTMENT_LOCATION_UPDATED" -> {
                    DepartmentLocationEvent event = objectMapper.readValue(message, DepartmentLocationEvent.class);
                    dimensionService.upsertDepartmentLocation(event);
                }
                default -> log.debug("Ignoring scheme event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process scheme event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "telemetry-service-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTelemetryEvents(String message) {
        log.info("[analytics] Received from telemetry-service-topic");
        try {
            String eventType = extractEventType(message);
            switch (eventType) {
                case "METER_READING_RECORDED" -> {
                    MeterReadingEvent event = objectMapper.readValue(message, MeterReadingEvent.class);
                    factService.ingestMeterReading(event);
                }
                case "WATER_QUANTITY_RECORDED" -> {
                    WaterQuantityEvent event = objectMapper.readValue(message, WaterQuantityEvent.class);
                    factService.ingestWaterQuantity(event);
                }
                case "SCHEME_PERFORMANCE_RECORDED" -> {
                    SchemePerformanceEvent event = objectMapper.readValue(message, SchemePerformanceEvent.class);
                    factService.ingestSchemePerformance(event);
                }
                default -> log.debug("Ignoring telemetry event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process telemetry event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "anomaly-service-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAnomalyEvents(String message) {
        log.info("[analytics] Received from anomaly-service-topic");
        try {
            String eventType = extractEventType(message);
            switch (eventType) {
                case "ESCALATION_CREATED", "ESCALATION_UPDATED" -> {
                    EscalationEvent event = objectMapper.readValue(message, EscalationEvent.class);
                    factService.ingestEscalation(event);
                }
                default -> log.debug("Ignoring anomaly event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process anomaly event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "common-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeCommonTopic(String message) {
        log.info("[analytics] Received from common-topic: {}", message);
    }

    private String extractEventType(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode eventTypeNode = node.get("eventType");
            return eventTypeNode != null ? eventTypeNode.asText() : "UNKNOWN";
        } catch (Exception e) {
            log.warn("Could not extract eventType from message, treating as UNKNOWN");
            return "UNKNOWN";
        }
    }
}
