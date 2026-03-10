package org.arghyam.jalsoochak.telemetry.service;

import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.telemetry.dto.requests.IntroRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.IssueReportRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.ManualReadingRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.MeterChangeRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.UpdatedPreviousReadingRequest;
import org.arghyam.jalsoochak.telemetry.dto.response.CreateReadingResponse;
import org.arghyam.jalsoochak.telemetry.dto.response.IntroResponse;
import org.arghyam.jalsoochak.telemetry.repository.TenantConfigRepository;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryConfirmedReadingSnapshot;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryOperatorWithSchema;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryPendingMeterChangeRecord;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryReadingRecord;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryTenantRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class GlificMeterWorkflowService {
    private static final String DEFAULT_ISSUE_PROMPT_ENGLISH =
            "Please select your issue by typing any of the number";
    private static final String DEFAULT_ISSUE_PROMPT_HINDI =
            "कृपया नंबर टाइप करके अपनी समस्या चुनें";
    private static final String DEFAULT_OTHERS_PROMPT_ENGLISH =
            "Please report your issue.";
    private static final String DEFAULT_OTHERS_PROMPT_HINDI =
            "कृपया अपनी समस्या बताएं।";
    private static final String LEGACY_ISSUE_PROMPT_ENGLISH =
            "Please type your issue in a few words.";
    private static final String LEGACY_ISSUE_PROMPT_HINDI =
            "कृपया अपनी समस्या संक्षेप में लिखें।";

    private static final List<String> DEFAULT_ISSUE_REASONS = List.of(
            "Meter Replaced",
            "Meter not working",
            "Meter damage",
            "Incorrect Reading Entered Previously",
            "Others"
    );

    private static final List<String> DEFAULT_ISSUE_REASONS_HINDI = List.of(
            "मीटर बदला गया",
            "मीटर काम नहीं कर रहा",
            "मीटर खराब है",
            "पहले गलत रीडिंग दर्ज हुई थी",
            "अन्य"
    );
    private static final List<String> DEFAULT_ISSUE_REASON_SELECTION_KEYS = List.of(
            "meterReplaced",
            "meterNotWorking",
            "meterDamage",
            "incorrectReadingEnteredPreviously",
            "others"
    );
    private static final List<String> TELEMETRY_ISSUE_REASONS = List.of(
            "Meter Replaced",
            "Meter not working",
            "Meter Damaged",
            "Others"
    );
    private static final List<String> TELEMETRY_ISSUE_REASONS_HINDI = List.of(
            "मीटर बदला गया",
            "मीटर काम नहीं कर रहा",
            "मीटर खराब है",
            "अन्य"
    );
    private static final List<String> TELEMETRY_ISSUE_REASON_SELECTION_KEYS = List.of(
            "meterReplaced",
            "meterNotWorking",
            "meterDamaged",
            "others"
    );

    private final GlificOperatorContextService operatorContextService;
    private final GlificLocalizationService localizationService;
    private final TenantConfigRepository tenantConfigRepository;
    private final GlificMessageTemplatesService templatesService;
    private final TelemetryTenantRepository telemetryTenantRepository;

    public GlificMeterWorkflowService(GlificOperatorContextService operatorContextService,
                                      GlificLocalizationService localizationService,
                                      TenantConfigRepository tenantConfigRepository,
                                      GlificMessageTemplatesService templatesService,
                                      TelemetryTenantRepository telemetryTenantRepository) {
        this.operatorContextService = operatorContextService;
        this.localizationService = localizationService;
        this.tenantConfigRepository = tenantConfigRepository;
        this.templatesService = templatesService;
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

            String defaultPrompt = "hindi".equals(languageKey) ? DEFAULT_ISSUE_PROMPT_HINDI : DEFAULT_ISSUE_PROMPT_ENGLISH;

            Optional<String> promptFromTemplate = templatesService.resolveScreenPrompt(tenantId, "ISSUE_REPORT", languageKey);
            String prompt;
            if (promptFromTemplate.isPresent()) {
                prompt = promptFromTemplate.get().trim();
            } else {
                prompt = tenantConfigRepository.findIssueReportPrompt(tenantId, languageKey)
                        .map(String::trim)
                        .filter(p -> !p.equalsIgnoreCase(LEGACY_ISSUE_PROMPT_ENGLISH)
                                && !p.equals(LEGACY_ISSUE_PROMPT_HINDI))
                        .orElse(defaultPrompt);
            }

            List<GlificMessageTemplatesService.TemplateOption> templateReasons =
                    templatesService.resolveScreenReasons(tenantId, "ISSUE_REPORT");
            List<String> reasons;
            if (!templateReasons.isEmpty()) {
                reasons = templateReasons.stream().map(r -> r.labelForLanguageKey(languageKey)).toList();
            } else {
                reasons = tenantConfigRepository.findIssueReportReasons(tenantId, languageKey);
                if (reasons.isEmpty()) {
                    reasons = "hindi".equals(languageKey) ? DEFAULT_ISSUE_REASONS_HINDI : DEFAULT_ISSUE_REASONS;
                }
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
            String languageKey = localizationService.normalizeLanguageKey(
                    operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId)
            );

            List<GlificMessageTemplatesService.TemplateOption> templateReasons =
                    templatesService.resolveScreenReasons(tenantId, "ISSUE_REPORT");
            List<String> reasons;
            if (!templateReasons.isEmpty()) {
                reasons = templateReasons.stream().map(r -> r.labelForLanguageKey(languageKey)).toList();
            } else {
                reasons = tenantConfigRepository.findIssueReportReasons(tenantId, languageKey);
                if (reasons.isEmpty()) {
                    reasons = "hindi".equals(languageKey) ? DEFAULT_ISSUE_REASONS_HINDI : DEFAULT_ISSUE_REASONS;
                }
            }
            String rawIssueReason = request.getIssueReason().trim();
            String resolvedIssueReason = resolveSelection(rawIssueReason, reasons).orElse(rawIssueReason);
            String selectedKey = resolveIssueSelectionKey(rawIssueReason, resolvedIssueReason, reasons, DEFAULT_ISSUE_REASON_SELECTION_KEYS);

            Long schemeId = telemetryTenantRepository
                    .findFirstSchemeForUser(operatorWithSchema.schemaName(), operatorWithSchema.operator().id())
                    .orElseThrow(() -> new IllegalStateException("Operator is not mapped to any scheme"));

            String correlationId = "issue-report-" + UUID.randomUUID();

            telemetryTenantRepository.createIssueReportRecord(
                    operatorWithSchema.schemaName(),
                    schemeId,
                    operatorWithSchema.operator().id(),
                    LocalDateTime.now(),
                    correlationId,
                    resolvedIssueReason
            );

            String fallbackMessage = "Issue reported. Thank you.";
            if ("hindi".equals(languageKey)) {
                fallbackMessage = "समस्या रिपोर्ट हो गई है। धन्यवाद।";
            }

            String message = templatesService
                    .resolveScreenConfirmationTemplate(tenantId, "ISSUE_REPORT", languageKey)
                    .or(() -> tenantConfigRepository.findIssueReportConfirmationTemplate(tenantId, languageKey))
                    .orElse(fallbackMessage);

            return IntroResponse.builder()
                    .success(true)
                    .message(message)
                    .correlationId(correlationId)
                    .selected(selectedKey)
                    .build();
        } catch (Exception e) {
            log.error("Error saving issue report for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Issue report could not be saved.")
                    .build();
        }
    }

    public IntroResponse othersPromptMessage(IntroRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = operatorContextService.resolveOperatorWithSchema(request.getContactId());
            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String languageKey = localizationService.normalizeLanguageKey(
                    operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId)
            );
            String message = "hindi".equals(languageKey) ? DEFAULT_OTHERS_PROMPT_HINDI : DEFAULT_OTHERS_PROMPT_ENGLISH;

            return IntroResponse.builder()
                    .success(true)
                    .message(message)
                    .build();
        } catch (Exception e) {
            log.error("Error preparing others prompt for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Others prompt could not be prepared.")
                    .build();
        }
    }

    public IntroResponse issueReportTelemetryPromptMessage(IntroRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = operatorContextService.resolveOperatorWithSchema(request.getContactId());
            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String languageKey = localizationService.normalizeLanguageKey(
                    operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId)
            );

            String prompt = "hindi".equals(languageKey) ? DEFAULT_ISSUE_PROMPT_HINDI : DEFAULT_ISSUE_PROMPT_ENGLISH;
            List<String> reasons = "hindi".equals(languageKey) ? TELEMETRY_ISSUE_REASONS_HINDI : TELEMETRY_ISSUE_REASONS;

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
            log.error("Error preparing telemetry issue report prompt for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Issue report prompt could not be prepared.")
                    .build();
        }
    }

    public IntroResponse issueReportTelemetrySubmitMessage(IssueReportRequest request) {
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
            String languageKey = localizationService.normalizeLanguageKey(
                    operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId)
            );

            List<String> reasons = "hindi".equals(languageKey) ? TELEMETRY_ISSUE_REASONS_HINDI : TELEMETRY_ISSUE_REASONS;
            String rawIssueReason = request.getIssueReason().trim();
            String resolvedIssueReason = resolveSelection(rawIssueReason, reasons).orElse(rawIssueReason);
            String selectedKey = resolveIssueSelectionKey(
                    rawIssueReason,
                    resolvedIssueReason,
                    reasons,
                    TELEMETRY_ISSUE_REASON_SELECTION_KEYS
            );

            Long schemeId = telemetryTenantRepository
                    .findFirstSchemeForUser(operatorWithSchema.schemaName(), operatorWithSchema.operator().id())
                    .orElseThrow(() -> new IllegalStateException("Operator is not mapped to any scheme"));

            String correlationId = "issue-report-" + UUID.randomUUID();
            telemetryTenantRepository.createIssueReportRecord(
                    operatorWithSchema.schemaName(),
                    schemeId,
                    operatorWithSchema.operator().id(),
                    LocalDateTime.now(),
                    correlationId,
                    resolvedIssueReason
            );

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
                    .selected(selectedKey)
                    .build();
        } catch (Exception e) {
            log.error("Error saving telemetry issue report for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Issue report could not be saved.")
                    .build();
        }
    }

    public IntroResponse othersSubmittedMessage(IssueReportRequest request) {
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

            String languageKey = localizationService.normalizeLanguageKey(
                    operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId)
            );
            String fallbackMessage = "Issue reported. Thank you.";
            if ("hindi".equals(languageKey)) {
                fallbackMessage = "समस्या रिपोर्ट हो गई है। धन्यवाद।";
            }
            String message = templatesService
                    .resolveScreenConfirmationTemplate(tenantId, "ISSUE_REPORT", languageKey)
                    .or(() -> tenantConfigRepository.findIssueReportConfirmationTemplate(tenantId, languageKey))
                    .orElse(fallbackMessage);

            return IntroResponse.builder()
                    .success(true)
                    .message(message)
                    .correlationId(correlationId)
                    .selected("others")
                    .build();
        } catch (Exception e) {
            log.error("Error saving others issue report for contactId {}: {}", request.getContactId(), e.getMessage(), e);
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
                String submittedReadingText = manualReadingValue.stripTrailingZeros().toPlainString();
                String previousReadingText = previousSnapshot.confirmedReading().stripTrailingZeros().toPlainString();
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
                        .message(localizationService.localizeMessage(
                                "Reading cannot be less than previous reading. Submitted reading: "
                                        + submittedReadingText + ". Previous reading: " + previousReadingText + ".",
                                languageKey
                        ))
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
                Optional<TelemetryReadingRecord> todaysReadingOpt =
                        telemetryTenantRepository.findLatestCompletedReadingForToday(
                                operatorWithSchema.schemaName(),
                                schemeId,
                                operatorWithSchema.operator().id()
                        );
                if (todaysReadingOpt.isPresent()) {
                    telemetryTenantRepository.updateConfirmedReading(
                            operatorWithSchema.schemaName(),
                            todaysReadingOpt.get().id(),
                            manualReadingValue,
                            operatorWithSchema.operator().id()
                    );
                    if (todaysReadingOpt.get().correlationId() != null
                            && !todaysReadingOpt.get().correlationId().isBlank()) {
                        correlationId = todaysReadingOpt.get().correlationId();
                    }
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

    public CreateReadingResponse updatePreviousReadingMessage(UpdatedPreviousReadingRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }
            if (request.getReading() == null || request.getReading().isBlank()) {
                throw new IllegalStateException("reading is required");
            }

            String normalizedReading = request.getReading().trim().replace(",", "");
            if (!normalizedReading.matches("^\\d+(\\.\\d+)?$")) {
                throw new IllegalStateException("reading must be numeric");
            }
            BigDecimal readingValue = new BigDecimal(normalizedReading);
            if (readingValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("reading must be greater than zero");
            }

            TelemetryOperatorWithSchema operatorWithSchema = operatorContextService.resolveOperatorWithSchema(request.getContactId());
            Long operatorId = operatorWithSchema.operator().id();

            Long schemeId = telemetryTenantRepository
                    .findFirstSchemeForUser(operatorWithSchema.schemaName(), operatorId)
                    .orElseThrow(() -> new IllegalStateException("Operator is not mapped to any scheme"));

            TelemetryReadingRecord previousDayRecord = telemetryTenantRepository
                    .findLatestCompletedReadingForPreviousDay(operatorWithSchema.schemaName(), schemeId, operatorId)
                    .orElseThrow(() -> new IllegalStateException("No previous day reading found to update"));

            telemetryTenantRepository.updateReadingValues(
                    operatorWithSchema.schemaName(),
                    previousDayRecord.id(),
                    readingValue,
                    operatorId
            );

            String correlationId = previousDayRecord.correlationId();
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = "previous-day-" + UUID.randomUUID();
            }

            return CreateReadingResponse.builder()
                    .success(true)
                    .message("Previous day reading updated successfully.")
                    .qualityStatus("CONFIRMED")
                    .correlationId(correlationId)
                    .meterReading(readingValue)
                    .build();
        } catch (Exception e) {
            log.error("Error updating previous day reading for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return CreateReadingResponse.builder()
                    .success(false)
                    .message("Previous day reading could not be updated.")
                    .qualityStatus("REJECTED")
                    .correlationId(request.getContactId())
                    .build();
        }
    }

    private Optional<String> resolveSelection(String rawSelection, List<String> options) {
        String value = rawSelection.trim();
        Integer index = parseSelectionIndex(value, options.size());
        if (index != null) {
            return Optional.of(options.get(index));
        }
        return options.stream().filter(v -> v.equalsIgnoreCase(value)).findFirst();
    }

    private String resolveIssueSelectionKey(String rawIssueReason,
                                            String resolvedIssueReason,
                                            List<String> reasons,
                                            List<String> selectionKeys) {
        String raw = rawIssueReason == null ? "" : rawIssueReason.trim();
        Integer index = parseSelectionIndex(raw, reasons.size());
        if (index != null) {
            if (index >= 0 && index < selectionKeys.size()) {
                return selectionKeys.get(index);
            }
            if (index >= 0 && index < reasons.size()) {
                return toLowerCamelToken(reasons.get(index));
            }
        }

        for (int i = 0; i < reasons.size(); i++) {
            if (reasons.get(i).equalsIgnoreCase(resolvedIssueReason)) {
                if (i < selectionKeys.size()) {
                    return selectionKeys.get(i);
                }
                return toLowerCamelToken(resolvedIssueReason);
            }
        }

        String selected = toLowerCamelToken(resolvedIssueReason);
        return selected == null || selected.isBlank() ? "others" : selected;
    }

    private Integer parseSelectionIndex(String rawSelection, int optionCount) {
        if (rawSelection == null || rawSelection.isBlank()) {
            return null;
        }

        String trimmed = rawSelection.trim();
        int digitEnd = 0;
        while (digitEnd < trimmed.length() && Character.isDigit(trimmed.charAt(digitEnd))) {
            digitEnd++;
        }
        if (digitEnd > 0) {
            int oneBased = Integer.parseInt(trimmed.substring(0, digitEnd));
            if (oneBased >= 1 && oneBased <= optionCount) {
                return oneBased - 1;
            }
            return null;
        }

        String normalized = trimmed.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{Nd}]+", "");
        int oneBased = switch (normalized) {
            case "one", "first", "ek", "एक" -> 1;
            case "two", "second", "do", "दो" -> 2;
            case "three", "third", "teen", "तीन" -> 3;
            case "four", "fourth", "char", "चार" -> 4;
            case "five", "fifth", "paanch", "panch", "पांच", "पाँच" -> 5;
            default -> -1;
        };
        if (oneBased >= 1 && oneBased <= optionCount) {
            return oneBased - 1;
        }
        return null;
    }

    private String toLowerCamelToken(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String sanitized = input.replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (sanitized.isBlank()) {
            return null;
        }
        String[] parts = sanitized.split("\\s+");
        if (parts.length == 0) {
            return null;
        }
        StringBuilder out = new StringBuilder(parts[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isBlank()) {
                continue;
            }
            String lower = parts[i].toLowerCase(Locale.ROOT);
            out.append(Character.toUpperCase(lower.charAt(0)));
            if (lower.length() > 1) {
                out.append(lower.substring(1));
            }
        }
        return out.toString();
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
