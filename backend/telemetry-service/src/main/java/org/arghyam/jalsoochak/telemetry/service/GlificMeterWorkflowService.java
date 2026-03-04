package org.arghyam.jalsoochak.telemetry.service;

import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.telemetry.dto.requests.IntroRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.IssueReportRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.ManualReadingRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.MeterChangeRequest;
import org.arghyam.jalsoochak.telemetry.dto.response.CreateReadingResponse;
import org.arghyam.jalsoochak.telemetry.dto.response.IntroResponse;
import org.arghyam.jalsoochak.telemetry.repository.TenantConfigRepository;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryConfirmedReadingSnapshot;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryOperatorWithSchema;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryPendingMeterChangeRecord;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryTenantRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class GlificMeterWorkflowService {

    private final GlificOperatorContextService operatorContextService;
    private final GlificLocalizationService localizationService;
    private final TenantConfigRepository tenantConfigRepository;
    private final TelemetryTenantRepository telemetryTenantRepository;

    public GlificMeterWorkflowService(GlificOperatorContextService operatorContextService,
                                      GlificLocalizationService localizationService,
                                      TenantConfigRepository tenantConfigRepository,
                                      TelemetryTenantRepository telemetryTenantRepository) {
        this.operatorContextService = operatorContextService;
        this.localizationService = localizationService;
        this.tenantConfigRepository = tenantConfigRepository;
        this.telemetryTenantRepository = telemetryTenantRepository;
    }

    public IntroResponse meterChangeMessage(IntroRequest request) {
        return meterChangeMessage(MeterChangeRequest.builder().contactId(request.getContactId()).build());
    }

    public IntroResponse meterChangeMessage(MeterChangeRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = operatorContextService.resolveOperatorWithSchema(request.getContactId());
            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String languageKey = localizationService.normalizeLanguageKey(operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId));

            String prompt = tenantConfigRepository.findMeterChangePrompt(tenantId, languageKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "meter change prompt is not configured. Add meter_change_prompt or meter_change_prompt_" + languageKey));

            List<String> reasons = tenantConfigRepository.findMeterChangeReasons(tenantId, languageKey);
            if (reasons.isEmpty()) {
                throw new IllegalStateException(
                        "No meter change reasons configured. Add meter_change_reason_1, meter_change_reason_2... or language-specific keys.");
            }

            StringBuilder message = new StringBuilder(prompt.trim());
            for (int i = 0; i < reasons.size(); i++) {
                message.append("\n")
                        .append(i + 1)
                        .append(". ")
                        .append(reasons.get(i));
            }

            return IntroResponse.builder()
                    .success(true)
                    .message(message.toString())
                    .build();
        } catch (Exception e) {
            log.error("Error building meter change reasons for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Meter change reasons could not be prepared.")
                    .build();
        }
    }

    public IntroResponse takeMeterReadingMessage(MeterChangeRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw new IllegalStateException("meter change reason selection is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = operatorContextService.resolveOperatorWithSchema(request.getContactId());

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String languageKey = localizationService.normalizeLanguageKey(operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId));

            List<String> reasons = tenantConfigRepository.findMeterChangeReasons(tenantId, languageKey);
            if (reasons.isEmpty()) {
                throw new IllegalStateException(
                        "No meter change reasons configured. Add meter_change_reason_1, meter_change_reason_2... or language-specific keys.");
            }
            String selectedReason = resolveSelection(request.getReason(), reasons)
                    .orElseThrow(() -> new IllegalStateException("Invalid meter change reason selection"));

            Long schemeId = telemetryTenantRepository
                    .findFirstSchemeForUser(operatorWithSchema.schemaName(), operatorWithSchema.operator().id())
                    .orElseThrow(() -> new IllegalStateException("Operator is not mapped to any scheme"));

            String correlationId = telemetryTenantRepository.upsertPendingMeterChangeRecord(
                    operatorWithSchema.schemaName(),
                    schemeId,
                    operatorWithSchema.operator().id(),
                    LocalDateTime.now(),
                    selectedReason
            );

            String prompt = tenantConfigRepository.findTakeMeterReadingPrompt(tenantId, languageKey)
                    .orElse("Please type your meter reading manually (numbers only).");

            return IntroResponse.builder()
                    .success(true)
                    .message(prompt)
                    .correlationId(correlationId)
                    .build();
        } catch (Exception e) {
            log.error("Error preparing take meter reading prompt for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Take meter reading prompt could not be prepared.")
                    .build();
        }
    }

    public IntroResponse issueReportPromptMessage(IntroRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = operatorContextService.resolveOperatorWithSchema(request.getContactId());

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String languageKey = localizationService.normalizeLanguageKey(operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId));

            String fallbackPrompt = "Please type your issue in a few words.";
            if ("hindi".equals(languageKey)) {
                fallbackPrompt = "कृपया अपनी समस्या संक्षेप में लिखें।";
            }

            String prompt = tenantConfigRepository.findIssueReportPrompt(tenantId, languageKey)
                    .orElse(fallbackPrompt);

            return IntroResponse.builder()
                    .success(true)
                    .message(prompt)
                    .build();
        } catch (Exception e) {
            log.error("Error preparing issue report prompt for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Issue report prompt could not be prepared.")
                    .build();
        }
    }

    public IntroResponse issueReportSubmitMessage(IssueReportRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }
            if (request.getIssueReason() == null || request.getIssueReason().isBlank()) {
                throw new IllegalStateException("issueReason is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = operatorContextService.resolveOperatorWithSchema(request.getContactId());

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }
            Long schemeId = telemetryTenantRepository
                    .findFirstSchemeForUser(operatorWithSchema.schemaName(), operatorWithSchema.operator().id())
                    .orElseThrow(() -> new IllegalStateException("Operator is not mapped to any scheme"));

            String correlationId = "issue-report-" + UUID.randomUUID();
            String issueReason = request.getIssueReason().trim();

            telemetryTenantRepository.createIssueReportRecord(
                    operatorWithSchema.schemaName(),
                    schemeId,
                    operatorWithSchema.operator().id(),
                    LocalDateTime.now(),
                    correlationId,
                    issueReason
            );

            String languageKey = localizationService.normalizeLanguageKey(operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId));

            String fallbackMessage = "Issue reported. Thank you.";
            if ("hindi".equals(languageKey)) {
                fallbackMessage = "समस्या रिपोर्ट हो गई है। धन्यवाद।";
            }

            String message = tenantConfigRepository.findIssueReportConfirmationTemplate(tenantId, languageKey)
                    .orElse(fallbackMessage);

            return IntroResponse.builder()
                    .success(true)
                    .message(message)
                    .correlationId(correlationId)
                    .build();
        } catch (Exception e) {
            log.error("Error saving issue report for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Issue report could not be saved.")
                    .build();
        }
    }

    public CreateReadingResponse manualReadingMessage(ManualReadingRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }
            if (request.getManualReading() == null || request.getManualReading().isBlank()) {
                throw new IllegalStateException("manualReading is required");
            }

            String normalizedReading = request.getManualReading().trim().replace(",", "");
            if (!normalizedReading.matches("^\\d+(\\.\\d+)?$")) {
                throw new IllegalStateException("manualReading must be numeric");
            }
            BigDecimal manualReadingValue = new BigDecimal(normalizedReading);
            if (manualReadingValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("manualReading must be greater than zero");
            }

            TelemetryOperatorWithSchema operatorWithSchema = operatorContextService.resolveOperatorWithSchema(request.getContactId());

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }
            String languageKey = localizationService.normalizeLanguageKey(operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId));

            Long schemeId = telemetryTenantRepository
                    .findFirstSchemeForUser(operatorWithSchema.schemaName(), operatorWithSchema.operator().id())
                    .orElseThrow(() -> new IllegalStateException("Operator is not mapped to any scheme"));

            Optional<TelemetryPendingMeterChangeRecord> pendingOpt = Optional.empty();
            String correlationId = (request.getCorrelationId() != null && !request.getCorrelationId().isBlank())
                    ? request.getCorrelationId().trim()
                    : "manual-" + UUID.randomUUID();

            if (request.getCorrelationId() != null && !request.getCorrelationId().isBlank()) {
                pendingOpt = telemetryTenantRepository.findPendingMeterChangeRecordByCorrelation(
                        operatorWithSchema.schemaName(),
                        schemeId,
                        operatorWithSchema.operator().id(),
                        correlationId
                );
            }

            Optional<TelemetryConfirmedReadingSnapshot> previousSnapshotOpt = telemetryTenantRepository
                    .findLatestConfirmedReadingSnapshot(operatorWithSchema.schemaName(), schemeId, null);

            if (previousSnapshotOpt.isPresent()
                    && manualReadingValue.compareTo(previousSnapshotOpt.get().confirmedReading()) < 0) {
                TelemetryConfirmedReadingSnapshot previousSnapshot = previousSnapshotOpt.get();
                telemetryTenantRepository.createAnomalyRecord(
                        operatorWithSchema.schemaName(),
                        AnomalyConstants.TYPE_READING_LESS_THAN_PREVIOUS,
                        operatorWithSchema.operator().id(),
                        schemeId,
                        pendingOpt.map(TelemetryPendingMeterChangeRecord::extractedReading).orElse(null),
                        null,
                        manualReadingValue,
                        0,
                        previousSnapshot.confirmedReading(),
                        previousSnapshot.createdAt(),
                        0,
                        "Manual reading is less than previous confirmed reading.",
                        AnomalyConstants.STATUS_OPEN
                );
                return CreateReadingResponse.builder()
                        .success(false)
                        .message(localizationService.localizeMessage("Manual reading cannot be less than previous reading.", languageKey))
                        .qualityStatus("REJECTED")
                        .correlationId(correlationId)
                        .meterReading(manualReadingValue)
                        .lastConfirmedReading(previousSnapshot.confirmedReading())
                        .build();
            }

            if (pendingOpt.isPresent()) {
                telemetryTenantRepository.updatePendingMeterChangeReading(
                        operatorWithSchema.schemaName(),
                        pendingOpt.get().id(),
                        manualReadingValue,
                        operatorWithSchema.operator().id()
                );
                correlationId = pendingOpt.get().correlationId();
            } else {
                telemetryTenantRepository.createFlowReading(
                        operatorWithSchema.schemaName(),
                        schemeId,
                        operatorWithSchema.operator().id(),
                        LocalDateTime.now(),
                        manualReadingValue,
                        manualReadingValue,
                        correlationId,
                        "",
                        request.getMeterChangeReason()
                );
            }

            int unreadableRetryCountToday = telemetryTenantRepository.countAnomaliesByTypeForToday(
                    operatorWithSchema.schemaName(),
                    operatorWithSchema.operator().id(),
                    schemeId,
                    AnomalyConstants.TYPE_UNREADABLE_IMAGE
            );

            telemetryTenantRepository.createAnomalyRecord(
                    operatorWithSchema.schemaName(),
                    AnomalyConstants.TYPE_MANUAL_OVERRIDE,
                    operatorWithSchema.operator().id(),
                    schemeId,
                    pendingOpt.map(TelemetryPendingMeterChangeRecord::extractedReading).orElse(null),
                    null,
                    manualReadingValue,
                    unreadableRetryCountToday,
                    previousSnapshotOpt.map(TelemetryConfirmedReadingSnapshot::confirmedReading).orElse(null),
                    previousSnapshotOpt.map(TelemetryConfirmedReadingSnapshot::createdAt).orElse(null),
                    0,
                    "Manual reading submitted as override.",
                    AnomalyConstants.STATUS_OPEN
            );

            int consecutiveOverrideDays = calculateConsecutiveDays(
                    telemetryTenantRepository.findAnomalyDatesByType(
                            operatorWithSchema.schemaName(),
                            operatorWithSchema.operator().id(),
                            schemeId,
                            AnomalyConstants.TYPE_MANUAL_OVERRIDE,
                            10
                    ),
                    LocalDate.now()
            );

            if (consecutiveOverrideDays >= 5) {
                telemetryTenantRepository.createAnomalyRecord(
                        operatorWithSchema.schemaName(),
                        AnomalyConstants.TYPE_CONSECUTIVE_OVERRIDE_5_DAYS,
                        operatorWithSchema.operator().id(),
                        schemeId,
                        pendingOpt.map(TelemetryPendingMeterChangeRecord::extractedReading).orElse(null),
                        null,
                        manualReadingValue,
                        0,
                        previousSnapshotOpt.map(TelemetryConfirmedReadingSnapshot::confirmedReading).orElse(null),
                        previousSnapshotOpt.map(TelemetryConfirmedReadingSnapshot::createdAt).orElse(null),
                        consecutiveOverrideDays,
                        "Manual overrides recorded for five or more consecutive days.",
                        AnomalyConstants.STATUS_OPEN
                );
            }

            CreateReadingResponse response = CreateReadingResponse.builder()
                    .success(true)
                    .correlationId(correlationId)
                    .meterReading(manualReadingValue)
                    .qualityStatus("CONFIRMED")
                    .build();

            String template = tenantConfigRepository.findManualReadingConfirmationTemplate(tenantId, languageKey)
                    .orElse("Manual reading {reading} saved successfully.");
            response.setMessage(template.replace("{reading}", manualReadingValue.stripTrailingZeros().toPlainString()));

            if (response.getCorrelationId() == null || response.getCorrelationId().isBlank()) {
                response.setCorrelationId(UUID.randomUUID().toString());
            }
            return response;
        } catch (Exception e) {
            log.error("Error processing manual reading for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            String languageKey = localizationService.resolveLanguageKeyForContact(request.getContactId());
            String descriptiveMessage = localizationService.resolveUserFacingErrorMessage(e, "Manual reading could not be saved.", languageKey);
            return CreateReadingResponse.builder()
                    .success(false)
                    .message(descriptiveMessage)
                    .qualityStatus("REJECTED")
                    .correlationId(request.getContactId())
                    .build();
        }
    }

    private Optional<String> resolveSelection(String rawSelection, List<String> options) {
        String value = rawSelection.trim();
        int digitEnd = 0;
        while (digitEnd < value.length() && Character.isDigit(value.charAt(digitEnd))) {
            digitEnd++;
        }
        if (digitEnd > 0) {
            int index = Integer.parseInt(value.substring(0, digitEnd));
            if (index >= 1 && index <= options.size()) {
                return Optional.of(options.get(index - 1));
            }
        }
        return options.stream().filter(v -> v.equalsIgnoreCase(value)).findFirst();
    }

    private int calculateConsecutiveDays(List<LocalDate> dates, LocalDate startDate) {
        if (dates == null || dates.isEmpty() || startDate == null) {
            return 0;
        }
        int count = 0;
        LocalDate cursor = startDate;
        for (LocalDate date : dates) {
            if (date == null) {
                continue;
            }
            if (date.isEqual(cursor)) {
                count++;
                cursor = cursor.minusDays(1);
            } else if (date.isBefore(cursor)) {
                break;
            }
        }
        return count;
    }
}
