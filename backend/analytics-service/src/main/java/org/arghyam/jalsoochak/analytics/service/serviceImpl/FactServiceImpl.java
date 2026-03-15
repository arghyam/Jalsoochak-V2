package org.arghyam.jalsoochak.analytics.service.serviceImpl;

import org.arghyam.jalsoochak.analytics.dto.event.EscalationEvent;
import org.arghyam.jalsoochak.analytics.dto.event.MeterReadingEvent;
import org.arghyam.jalsoochak.analytics.dto.event.SchemePerformanceEvent;
import org.arghyam.jalsoochak.analytics.dto.event.TenantEscalationEvent;
import org.arghyam.jalsoochak.analytics.dto.event.WaterQuantityEvent;
import org.arghyam.jalsoochak.analytics.entity.Anomaly;
import org.arghyam.jalsoochak.analytics.entity.DimTenant;
import org.arghyam.jalsoochak.analytics.entity.FactEscalation;
import org.arghyam.jalsoochak.analytics.entity.FactMeterReading;
import org.arghyam.jalsoochak.analytics.entity.FactSchemePerformance;
import org.arghyam.jalsoochak.analytics.entity.FactWaterQuantity;
import org.arghyam.jalsoochak.analytics.repository.AnomalyRepository;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.FactEscalationRepository;
import org.arghyam.jalsoochak.analytics.repository.FactMeterReadingRepository;
import org.arghyam.jalsoochak.analytics.repository.FactSchemePerformanceRepository;
import org.arghyam.jalsoochak.analytics.repository.FactWaterQuantityRepository;
import org.arghyam.jalsoochak.analytics.service.FactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * Sentinel value used when an operator has never uploaded a reading.
     * Must match the value produced by EscalationSchedulerService (tenant-service).
     */
    public static final String LAST_RECORDED_BFM_DATE_NEVER = "Never";

    private final FactMeterReadingRepository meterReadingRepository;
    private final FactWaterQuantityRepository waterQuantityRepository;
    private final FactEscalationRepository escalationRepository;
    private final FactSchemePerformanceRepository schemePerformanceRepository;
    private final AnomalyRepository anomalyRepository;
    private final DimTenantRepository dimTenantRepository;

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
        ensureTenantExists(event.getTenantId(), null);
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
        ensureTenantExists(event.getTenantId(), event.getTenantSchema());
        OffsetDateTime now = OffsetDateTime.now();
        int operatorCount = event.getOperators() != null ? event.getOperators().size() : 0;
        int escalationRowsCreated = 0;
        int anomalyRowsCreated = 0;

        // One fact_escalation_table row + one anomaly_table row per operator
        if (event.getOperators() == null) return;
        for (TenantEscalationEvent.TenantOperatorEscalationDetail op : event.getOperators()) {
            if (op.getCorrelationId() == null || op.getCorrelationId().isBlank()) {
                log.warn("Skipping operator escalation row — correlationId is null/blank (userId={} name={})", op.getUserId(), op.getName());
                continue;
            }
            if (op.getUserId() == null) {
                log.warn("Skipping operator escalation row — userId is null (correlationId={})", op.getCorrelationId());
                continue;
            }
            if (op.getConsecutiveDaysMissed() == null) {
                log.warn("Skipping operator escalation row — consecutiveDaysMissed is null (correlationId={})", op.getCorrelationId());
                continue;
            }

            Integer schemeId = null;
            try {
                if (op.getSchemeId() != null && !op.getSchemeId().isBlank()) {
                    schemeId = Integer.parseInt(op.getSchemeId());
                }
            } catch (NumberFormatException e) {
                log.warn("Could not parse schemeId '{}' for operator row", op.getSchemeId());
            }

            // Idempotency: skip if this operator event was already ingested
            if (escalationRepository.existsByCorrelationId(op.getCorrelationId())) {
                log.debug("Skipping duplicate fact_escalation for correlationId={}", op.getCorrelationId());
            } else {
                FactEscalation escalationFact = FactEscalation.builder()
                        .tenantId(event.getTenantId())
                        .schemeId(schemeId)
                        .escalationType(1) // NO_SUBMISSION
                        .message(op.getName() + " has not submitted for " + op.getConsecutiveDaysMissed() + " consecutive days")
                        .correlationId(op.getCorrelationId())
                        .userId(op.getUserId())
                        .resolutionStatus(1) // UNRESOLVED
                        .remark(null)
                        .createdAt(now.toLocalDateTime())
                        .updatedAt(now.toLocalDateTime())
                        .build();
                escalationRepository.save(escalationFact);
                escalationRowsCreated++;
            }

            if (anomalyRepository.existsByCorrelationIdAndTypeAndSchemeIdAndTenantId(
                    op.getCorrelationId(), 6, schemeId, event.getTenantId())) {
                log.debug("Skipping duplicate anomaly for correlationId={}", op.getCorrelationId());
                continue;
            }

            LocalDate previousReadingDate = null;
            if (op.getLastRecordedBfmDate() != null && !op.getLastRecordedBfmDate().isBlank()
                    && !LAST_RECORDED_BFM_DATE_NEVER.equalsIgnoreCase(op.getLastRecordedBfmDate())) {
                previousReadingDate = parseDateOrNull(op.getLastRecordedBfmDate());
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
            anomalyRowsCreated++;
        }
        log.info("Processed {} operators for tenant={}: {} escalation rows, {} anomaly rows created (of {} total)",
                operatorCount, event.getTenantId(), escalationRowsCreated, anomalyRowsCreated, operatorCount);
    }

    private void ensureTenantExists(Integer tenantId, String tenantSchema) {
        if (tenantId == null) return;
        if (dimTenantRepository.existsById(tenantId)) return;
        String stateCode = (tenantSchema != null && tenantSchema.startsWith("tenant_"))
                ? tenantSchema.substring("tenant_".length())
                : "unknown";
        LocalDateTime now = LocalDateTime.now();
        DimTenant stub = DimTenant.builder()
                .tenantId(tenantId)
                .stateCode(stateCode)
                .title(tenantSchema != null ? tenantSchema : "tenant-" + tenantId)
                .countryCode("IN")
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            dimTenantRepository.save(stub);
            log.info("Created dim_tenant stub [id={} stateCode={}] — full data expected via TENANT_CREATED event", tenantId, stateCode);
        } catch (DataIntegrityViolationException e) {
            // Another thread inserted first — treat as success
            log.debug("dim_tenant stub for id={} already inserted by concurrent thread, continuing", tenantId);
        }
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

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            log.warn("Could not parse date '{}', storing null", value);
            return null;
        }
    }
}
