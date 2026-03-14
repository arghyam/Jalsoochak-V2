package org.arghyam.jalsoochak.analytics.service.serviceImpl;

import org.arghyam.jalsoochak.analytics.dto.event.EscalationEvent;
import org.arghyam.jalsoochak.analytics.dto.event.MeterReadingEvent;
import org.arghyam.jalsoochak.analytics.dto.event.SchemePerformanceEvent;
import org.arghyam.jalsoochak.analytics.dto.event.TenantEscalationEvent;
import org.arghyam.jalsoochak.analytics.dto.event.WaterQuantityEvent;
import org.arghyam.jalsoochak.analytics.entity.Anomaly;
import org.arghyam.jalsoochak.analytics.entity.FactEscalation;
import org.arghyam.jalsoochak.analytics.entity.FactMeterReading;
import org.arghyam.jalsoochak.analytics.entity.FactSchemePerformance;
import org.arghyam.jalsoochak.analytics.entity.FactWaterQuantity;
import org.arghyam.jalsoochak.analytics.repository.AnomalyRepository;
import org.arghyam.jalsoochak.analytics.repository.FactEscalationRepository;
import org.arghyam.jalsoochak.analytics.repository.FactMeterReadingRepository;
import org.arghyam.jalsoochak.analytics.repository.FactSchemePerformanceRepository;
import org.arghyam.jalsoochak.analytics.repository.FactWaterQuantityRepository;
import org.arghyam.jalsoochak.analytics.service.FactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactServiceImpl implements FactService {

    private final FactMeterReadingRepository meterReadingRepository;
    private final FactWaterQuantityRepository waterQuantityRepository;
    private final FactEscalationRepository escalationRepository;
    private final FactSchemePerformanceRepository schemePerformanceRepository;
    private final AnomalyRepository anomalyRepository;

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
                .submissionStatus(event.getSubmissionStatus())
                .outageReason(event.getOutageReason())
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

    @Override
    @Transactional
    public void ingestTenantEscalation(TenantEscalationEvent event) {
        OffsetDateTime now = OffsetDateTime.now();
        int operatorCount = event.getOperators() != null ? event.getOperators().size() : 0;

        // One fact_escalation_table row per officer event (no single scheme)
        FactEscalation escalationFact = FactEscalation.builder()
                .tenantId(event.getTenantId())
                .schemeId(null)
                .escalationType(1) // NO_SUBMISSION
                .message(operatorCount + " operator(s) have not submitted for \u2265" + event.getEscalationLevel() + " days")
                .correlationId(event.getCorrelationId())
                .userId(event.getOfficerId() != null ? event.getOfficerId().intValue() : null)
                .resolutionStatus(1) // UNRESOLVED
                .remark(null)
                .createdAt(now.toLocalDateTime())
                .updatedAt(now.toLocalDateTime())
                .build();
        escalationRepository.save(escalationFact);
        log.info("Ingested fact_escalation_table for tenant={} officer={} operators={}",
                event.getTenantId(), event.getOfficerId(), operatorCount);

        // One anomaly_table row per operator
        if (event.getOperators() == null) return;
        for (TenantEscalationEvent.TenantOperatorEscalationDetail op : event.getOperators()) {
            Integer schemeId = null;
            try {
                if (op.getSchemeId() != null && !op.getSchemeId().isBlank()) {
                    schemeId = Integer.parseInt(op.getSchemeId());
                }
            } catch (NumberFormatException e) {
                log.warn("Could not parse schemeId '{}' for anomaly row", op.getSchemeId());
            }

            LocalDate previousReadingDate = null;
            if (op.getLastRecordedBfmDate() != null && !op.getLastRecordedBfmDate().isBlank()
                    && !"Never".equalsIgnoreCase(op.getLastRecordedBfmDate())) {
                previousReadingDate = parseDate(op.getLastRecordedBfmDate());
            }

            BigDecimal previousReading = op.getLastConfirmedReading() != null
                    ? BigDecimal.valueOf(op.getLastConfirmedReading()) : null;

            Anomaly anomaly = Anomaly.builder()
                    .uuid(UUID.randomUUID().toString())
                    .type(6) // NO_SUBMISSION
                    .userId(op.getUserId())
                    .schemeId(schemeId)
                    .tenantId(event.getTenantId())
                    .previousReading(previousReading)
                    .previousReadingDate(previousReadingDate)
                    .consecutiveDaysMissed(op.getConsecutiveDaysMissed())
                    .reason("No submission for " + op.getConsecutiveDaysMissed() + " consecutive days")
                    .status(1) // OPEN
                    .correlationId(op.getCorrelationId())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            anomalyRepository.save(anomaly);
        }
        log.info("Ingested {} anomaly row(s) for tenant={}", operatorCount, event.getTenantId());
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
