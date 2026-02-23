package com.example.analytics.service.serviceImpl;

import com.example.analytics.dto.event.EscalationEvent;
import com.example.analytics.dto.event.MeterReadingEvent;
import com.example.analytics.dto.event.SchemePerformanceEvent;
import com.example.analytics.dto.event.WaterQuantityEvent;
import com.example.analytics.entity.FactEscalation;
import com.example.analytics.entity.FactMeterReading;
import com.example.analytics.entity.FactSchemePerformance;
import com.example.analytics.entity.FactWaterQuantity;
import com.example.analytics.repository.FactEscalationRepository;
import com.example.analytics.repository.FactMeterReadingRepository;
import com.example.analytics.repository.FactSchemePerformanceRepository;
import com.example.analytics.repository.FactWaterQuantityRepository;
import com.example.analytics.service.FactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactServiceImpl implements FactService {

    private final FactMeterReadingRepository meterReadingRepository;
    private final FactWaterQuantityRepository waterQuantityRepository;
    private final FactEscalationRepository escalationRepository;
    private final FactSchemePerformanceRepository schemePerformanceRepository;

    @Override
    @Transactional
    public void ingestMeterReading(MeterReadingEvent event) {
        LocalDateTime readingAt = parseTimestamp(event.getReadingAt());
        LocalDate readingDate = parseDate(event.getReadingDate());

        FactMeterReading fact = FactMeterReading.builder()
                .tenantId(event.getTenantId())
                .schemeId(event.getSchemeId())
                .userId(event.getUserId())
                .extractedReading(event.getExtractedReading())
                .confirmedReading(event.getConfirmedReading())
                .confidence(event.getConfidence())
                .imageUrl(event.getImageUrl())
                .readingAt(readingAt)
                .channel(event.getChannel())
                .readingDate(readingDate)
                .createdAt(LocalDateTime.now())
                .build();

        meterReadingRepository.save(fact);
        log.info("Ingested fact_meter_reading_table for scheme={} tenant={}", event.getSchemeId(), event.getTenantId());
    }

    @Override
    @Transactional
    public void ingestWaterQuantity(WaterQuantityEvent event) {
        LocalDate date = parseDate(event.getDate());
        LocalDateTime now = LocalDateTime.now();

        FactWaterQuantity fact = FactWaterQuantity.builder()
                .tenantId(event.getTenantId())
                .schemeId(event.getSchemeId())
                .userId(event.getUserId())
                .waterQuantity(event.getWaterQuantity())
                .date(date)
                .createdAt(now)
                .updatedAt(now)
                .build();

        waterQuantityRepository.save(fact);
        log.info("Ingested fact_water_quantity_table for scheme={} tenant={}", event.getSchemeId(), event.getTenantId());
    }

    @Override
    @Transactional
    public void ingestEscalation(EscalationEvent event) {
        LocalDateTime now = LocalDateTime.now();

        FactEscalation fact = FactEscalation.builder()
                .tenantId(event.getTenantId())
                .schemeId(event.getSchemeId())
                .escalationType(event.getEscalationType())
                .message(event.getMessage())
                .userId(event.getUserId())
                .resolutionStatus(event.getResolutionStatus())
                .remark(event.getRemark())
                .createdAt(now)
                .updatedAt(now)
                .build();

        escalationRepository.save(fact);
        log.info("Ingested fact_escalation_table for scheme={} tenant={}", event.getSchemeId(), event.getTenantId());
    }

    @Override
    @Transactional
    public void ingestSchemePerformance(SchemePerformanceEvent event) {
        LocalDate lastSupplyDate = parseDate(event.getLastWaterSupplyDate());
        LocalDateTime now = LocalDateTime.now();

        FactSchemePerformance fact = FactSchemePerformance.builder()
                .tenantId(event.getTenantId())
                .schemeId(event.getSchemeId())
                .performanceScore(event.getPerformanceScore())
                .lastWaterSupplyDate(lastSupplyDate)
                .createdAt(now)
                .updatedAt(now)
                .build();

        schemePerformanceRepository.save(fact);
        log.info("Ingested fact_scheme_performance_table for scheme={} tenant={}", event.getSchemeId(), event.getTenantId());
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            log.warn("Could not parse timestamp '{}', falling back to now", value);
            return LocalDateTime.now();
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            log.warn("Could not parse date '{}', falling back to today", value);
            return LocalDate.now();
        }
    }
}
