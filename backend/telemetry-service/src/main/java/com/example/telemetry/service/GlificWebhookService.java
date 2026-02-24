package com.example.telemetry.service;

import com.example.telemetry.dto.response.ClosingResponse;
import com.example.telemetry.dto.response.CreateReadingResponse;
import com.example.telemetry.dto.response.IntroResponse;
import com.example.telemetry.dto.requests.ClosingRequest;
import com.example.telemetry.dto.requests.CreateReadingRequest;
import com.example.telemetry.dto.requests.GlificWebhookRequest;
import com.example.telemetry.dto.requests.IntroRequest;
import com.example.telemetry.dto.requests.ManualReadingRequest;
import com.example.telemetry.dto.requests.MeterChangeRequest;
import com.example.telemetry.dto.requests.SelectedChannelRequest;
import com.example.telemetry.dto.requests.SelectedItemRequest;
import com.example.telemetry.dto.requests.SelectedLanguageRequest;
import com.example.telemetry.repository.TenantConfigRepository;
import com.example.telemetry.repository.TelemetryOperatorWithSchema;
import com.example.telemetry.repository.TelemetryTenantRepository;
import com.example.telemetry.dto.response.SelectionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class GlificWebhookService {

    private final MinioService minioService;
    private final RestTemplate restTemplate;
    private final BfmReadingService bfmReadingService;
    private final TelemetryTenantRepository telemetryTenantRepository;
    private final TenantConfigRepository tenantConfigRepository;

    private final String glificApiToken;

    public GlificWebhookService(MinioService minioService,
                                RestTemplate restTemplate,
                                BfmReadingService bfmReadingService,
                                TelemetryTenantRepository telemetryTenantRepository,
                                TenantConfigRepository tenantConfigRepository,
                                @Value("${glific.api-token:}") String glificApiToken) {
        this.minioService = minioService;
        this.restTemplate = restTemplate;
        this.bfmReadingService = bfmReadingService;
        this.telemetryTenantRepository = telemetryTenantRepository;
        this.tenantConfigRepository = tenantConfigRepository;
        this.glificApiToken = glificApiToken;
    }

    public CreateReadingResponse processImage(GlificWebhookRequest glificWebhookRequest) {
        try {
            String contactId = glificWebhookRequest.getContactId();
            String mediaId = glificWebhookRequest.getMediaId();
            String mediaUrl = glificWebhookRequest.getMediaUrl();

            boolean hasImage = (mediaId != null && !mediaId.isBlank()) || (mediaUrl != null && !mediaUrl.isBlank());
            if (!hasImage) {
                return CreateReadingResponse.builder()
                        .success(false)
                        .message("Invalid media. Please send a clear meter image.")
                        .qualityStatus("REJECTED")
                        .correlationId(contactId)
                        .build();
            }

            byte[] imageBytes = mediaId != null && !mediaId.isBlank()
                    ? downloadImageFromGlific(mediaId)
                    : downloadImage(mediaUrl);

            log.info("imageBytes: {}", imageBytes);

            String objectKey = "bfm/" + contactId + "/" + System.currentTimeMillis() + ".jpg";
            String imageStorageUrl = minioService.upload(imageBytes, objectKey);
            log.info("imageStorage url: {}", imageStorageUrl);

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(contactId)
                    .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + contactId));

            Long schemeId = telemetryTenantRepository
                    .findFirstSchemeForUser(operatorWithSchema.schemaName(), operatorWithSchema.operator().id())
                    .orElseThrow(() -> new IllegalStateException("Operator is not mapped to any scheme"));

            CreateReadingRequest createReadingRequest = CreateReadingRequest.builder()
                    .schemeId(schemeId)
                    .operatorId(operatorWithSchema.operator().id())
                    .readingUrl(imageStorageUrl)
                    .readingValue(null)
                    .readingTime(null)
                    .build();

            return bfmReadingService.createReading(
                    createReadingRequest,
                    operatorWithSchema.schemaName(),
                    operatorWithSchema.operator(),
                    contactId
            );
        } catch (Exception e) {
            log.error("Unexpected error processing image for contactId {}: {}", glificWebhookRequest.getContactId(), e.getMessage(), e);
            return CreateReadingResponse.builder()
                    .success(false)
                    .message("Something went wrong. Please try again.")
                    .qualityStatus("REJECTED")
                    .correlationId(glificWebhookRequest.getContactId())
                    .build();
        }
    }

    public IntroResponse introMessage(IntroRequest introRequest) {
        try {
            if (introRequest.getContactId() == null || introRequest.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(introRequest.getContactId())
                    .orElseThrow(() -> new IllegalStateException("Operator not found"));

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String name = operatorWithSchema.operator().title() != null && !operatorWithSchema.operator().title().isBlank()
                    ? operatorWithSchema.operator().title()
                    : "there";

            Optional<String> selectedLanguageOpt = Optional.of(resolveOperatorLanguage(operatorWithSchema, tenantId));
            String languageKey = selectedLanguageOpt
                    .map(this::normalizeLanguageKey)
                    .orElse("english");

            String template;
            if (selectedLanguageOpt.isPresent() && !"english".equals(languageKey)) {
                template = tenantConfigRepository
                        .findConfigValue(tenantId, "intro_message_" + languageKey)
                        .orElseThrow(() -> new IllegalStateException(
                                "Intro message is not configured for selected language. Add intro_message_" + languageKey));
            } else {
                template = tenantConfigRepository
                        .findConfigValue(tenantId, "intro_message_" + languageKey)
                        .or(() -> tenantConfigRepository.findConfigValue(tenantId, "intro_message"))
                        .orElseThrow(() -> new IllegalStateException(
                                "Intro message is not configured. Add intro_message_" + languageKey + " or intro_message"));
            }

            log.info("Resolved intro message config for contactId {} with language key '{}'", introRequest.getContactId(), languageKey);

            return IntroResponse.builder()
                    .success(true)
                    .message(template.replace("{name}", name))
                    .build();
        } catch (Exception e) {
            log.error("Error sending intro message for contactId {}: {}", introRequest.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Something went wrong. Please try again.")
                    .build();
        }
    }

    public ClosingResponse closingMessage(ClosingRequest closingRequest) {
        try {
            if (closingRequest.getContactId() == null || closingRequest.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(closingRequest.getContactId())
                    .orElseThrow(() -> new IllegalStateException("Operator not found"));

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            Optional<String> selectedLanguageOpt = Optional.of(resolveOperatorLanguage(operatorWithSchema, tenantId));
            String languageKey = selectedLanguageOpt
                    .map(this::normalizeLanguageKey)
                    .orElse("english");

            String template;
            if (selectedLanguageOpt.isPresent() && !"english".equals(languageKey)) {
                template = tenantConfigRepository
                        .findConfigValue(tenantId, "closing_message_" + languageKey)
                        .orElseThrow(() -> new IllegalStateException(
                                "Closing message is not configured for selected language. Add closing_message_" + languageKey));
            } else {
                template = tenantConfigRepository
                        .findConfigValue(tenantId, "closing_message_" + languageKey)
                        .or(() -> tenantConfigRepository.findConfigValue(tenantId, "closing_message"))
                        .orElseThrow(() -> new IllegalStateException(
                                "Closing message is not configured. Add closing_message_" + languageKey + " or closing_message"));
            }

            log.info("Resolved closing message config for contactId {} with language key '{}'", closingRequest.getContactId(), languageKey);

            return ClosingResponse.builder()
                    .success(true)
                    .message(template)
                    .build();
        } catch (Exception e) {
            log.error("Error sending closing message for contactId {}: {}", closingRequest.getContactId(), e.getMessage(), e);
            return ClosingResponse.builder()
                    .success(false)
                    .message("Something went wrong. Please try again.")
                    .build();
        }
    }

    public IntroResponse languageSelectionMessage(IntroRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(request.getContactId())
                    .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + request.getContactId()));

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String prompt = tenantConfigRepository.findLanguageSelectionPrompt(tenantId)
                    .orElseThrow(() -> new IllegalStateException("language_selection_prompt is not configured"));

            java.util.List<String> languageOptions = tenantConfigRepository.findLanguageOptions(tenantId);
            if (languageOptions.isEmpty()) {
                throw new IllegalStateException("No language options configured. Add language_1, language_2, ...");
            }

            StringBuilder message = new StringBuilder(prompt.trim());
            for (int i = 0; i < languageOptions.size(); i++) {
                message.append("\n")
                        .append(i + 1)
                        .append(". ")
                        .append(languageOptions.get(i));
            }

            return IntroResponse.builder()
                    .success(true)
                    .message(message.toString())
                    .build();
        } catch (Exception e) {
            log.error("Error building language selection message for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Language selection could not be prepared.")
                    .build();
        }
    }

    public IntroResponse selectedLanguageMessage(SelectedLanguageRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }
            if (request.getLanguage() == null || request.getLanguage().isBlank()) {
                throw new IllegalStateException("language selection is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(request.getContactId())
                    .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + request.getContactId()));

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            List<String> languageOptions = tenantConfigRepository.findLanguageOptions(tenantId);
            if (languageOptions.isEmpty()) {
                throw new IllegalStateException("No language options configured for tenant");
            }

            String selectedLanguage = resolveLanguageSelection(request.getLanguage(), languageOptions)
                    .orElseThrow(() -> new IllegalStateException("Invalid language selection"));

            int selectedLanguageId = languageOptions.indexOf(selectedLanguage) + 1;
            telemetryTenantRepository.updateUserLanguageId(
                    operatorWithSchema.schemaName(),
                    operatorWithSchema.operator().id(),
                    selectedLanguageId
            );

            String languageSpecificKey = "language_selection_confirmation_template_" + normalizeLanguageKey(selectedLanguage);
            String confirmationTemplate = tenantConfigRepository
                    .findConfigValue(tenantId, languageSpecificKey)
                    .or(() -> tenantConfigRepository.findConfigValue(tenantId, "language_selection_confirmation_template"))
                    .orElse("Language selected: {language}");

            return IntroResponse.builder()
                    .success(true)
                    .message(confirmationTemplate.replace("{language}", selectedLanguage))
                    .build();
        } catch (Exception e) {
            log.error("Error saving selected language for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Language selection could not be saved.")
                    .build();
        }
    }

    public IntroResponse channelSelectionMessage(IntroRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(request.getContactId())
                    .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + request.getContactId()));

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String selectedLanguage = resolveOperatorLanguage(operatorWithSchema, tenantId);
            String languageKey = normalizeLanguageKey(selectedLanguage);

            String prompt = tenantConfigRepository.findChannelSelectionPrompt(tenantId, languageKey)
                    .orElse("Please select your preferred channel by typing the corresponding number:");

            List<String> channelOptions = tenantConfigRepository.findChannelOptions(tenantId, languageKey);
            if (channelOptions.isEmpty()) {
                throw new IllegalStateException("No channel options configured. Add channel_1/channel_2 or language-specific keys.");
            }

            StringBuilder message = new StringBuilder(prompt.trim());
            for (int i = 0; i < channelOptions.size(); i++) {
                message.append("\n")
                        .append(i + 1)
                        .append(". ")
                        .append(channelOptions.get(i));
            }

            return IntroResponse.builder()
                    .success(true)
                    .message(message.toString())
                    .build();
        } catch (Exception e) {
            log.error("Error building channel selection message for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Channel selection could not be prepared.")
                    .build();
        }
    }

    public IntroResponse selectedChannelMessage(SelectedChannelRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }
            if (request.getChannel() == null || request.getChannel().isBlank()) {
                throw new IllegalStateException("channel selection is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(request.getContactId())
                    .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + request.getContactId()));

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String selectedLanguage = resolveOperatorLanguage(operatorWithSchema, tenantId);
            String languageKey = normalizeLanguageKey(selectedLanguage);

            List<String> channelOptions = tenantConfigRepository.findChannelOptions(tenantId, languageKey);
            if (channelOptions.isEmpty()) {
                throw new IllegalStateException("No channel options configured for tenant");
            }

            String selectedChannel = resolveLanguageSelection(request.getChannel(), channelOptions)
                    .orElseThrow(() -> new IllegalStateException("Invalid channel selection"));

            int selectedChannelId = channelOptions.indexOf(selectedChannel) + 1;
            Long schemeId = telemetryTenantRepository
                    .findFirstSchemeForUser(operatorWithSchema.schemaName(), operatorWithSchema.operator().id())
                    .orElseThrow(() -> new IllegalStateException("Operator is not mapped to any scheme"));
            telemetryTenantRepository.updateSchemeChannel(operatorWithSchema.schemaName(), schemeId, selectedChannelId);

            String confirmationTemplate = tenantConfigRepository
                    .findConfigValue(tenantId, "channel_selection_confirmation_template_" + languageKey)
                    .or(() -> tenantConfigRepository.findConfigValue(tenantId, "channel_selection_confirmation_template"))
                    .orElse("Channel selected: {channel}");

            return IntroResponse.builder()
                    .success(true)
                    .message(confirmationTemplate.replace("{channel}", selectedChannel))
                    .build();
        } catch (Exception e) {
            log.error("Error saving selected channel for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Channel selection could not be saved.")
                    .build();
        }
    }

    public IntroResponse itemSelectionMessage(IntroRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(request.getContactId())
                    .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + request.getContactId()));

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String selectedLanguage = resolveOperatorLanguage(operatorWithSchema, tenantId);
            String languageKey = normalizeLanguageKey(selectedLanguage);

            String prompt = tenantConfigRepository.findItemSelectionPrompt(tenantId, languageKey)
                    .orElse("Please select what you want to do:");

            List<String> itemOptions = tenantConfigRepository.findItemOptions(tenantId, languageKey);
            if (itemOptions.isEmpty()) {
                throw new IllegalStateException("No item options configured. Add item_1/item_2... or language-specific keys.");
            }

            StringBuilder message = new StringBuilder(prompt.trim());
            for (int i = 0; i < itemOptions.size(); i++) {
                message.append("\n")
                        .append(i + 1)
                        .append(". ")
                        .append(itemOptions.get(i));
            }

            return IntroResponse.builder()
                    .success(true)
                    .message(message.toString())
                    .build();
        } catch (Exception e) {
            log.error("Error building item selection message for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Item selection could not be prepared.")
                    .build();
        }
    }

    public SelectionResponse selectedItemMessage(SelectedItemRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }
            if (request.getChannel() == null || request.getChannel().isBlank()) {
                throw new IllegalStateException("item selection is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(request.getContactId())
                    .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + request.getContactId()));

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String selectedLanguage = resolveOperatorLanguage(operatorWithSchema, tenantId);
            String languageKey = normalizeLanguageKey(selectedLanguage);

            List<String> itemOptions = tenantConfigRepository.findItemOptions(tenantId, languageKey);
            if (itemOptions.isEmpty()) {
                throw new IllegalStateException("No item options configured for tenant");
            }

            String selectedItemLabel = resolveLanguageSelection(request.getChannel(), itemOptions)
                    .orElseThrow(() -> new IllegalStateException("Invalid item selection"));

            String selectedCode = toItemCode(selectedItemLabel, itemOptions);

            String template = tenantConfigRepository
                    .findConfigValue(tenantId, "item_selection_confirmation_template_" + languageKey)
                    .or(() -> tenantConfigRepository.findConfigValue(tenantId, "item_selection_confirmation_template"))
                    .orElse("{item} selected");

            String message = template
                    .replace("{item}", selectedItemLabel)
                    .replace("{selected}", selectedCode);

            return SelectionResponse.builder()
                    .success(true)
                    .selected(selectedCode)
                    .message(message)
                    .build();
        } catch (Exception e) {
            log.error("Error processing selected item for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return SelectionResponse.builder()
                    .success(false)
                    .selected(null)
                    .message("Item selection could not be saved.")
                    .build();
        }
    }

    public IntroResponse meterChangeMessage(IntroRequest request) {
        return meterChangeMessage(MeterChangeRequest.builder().contactId(request.getContactId()).build());
    }

    public IntroResponse meterChangeMessage(MeterChangeRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(request.getContactId())
                    .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + request.getContactId()));

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String selectedLanguage = resolveOperatorLanguage(operatorWithSchema, tenantId);
            String languageKey = normalizeLanguageKey(selectedLanguage);

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

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(request.getContactId())
                    .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + request.getContactId()));

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String selectedLanguage = resolveOperatorLanguage(operatorWithSchema, tenantId);
            String languageKey = normalizeLanguageKey(selectedLanguage);

            List<String> reasons = tenantConfigRepository.findMeterChangeReasons(tenantId, languageKey);
            if (reasons.isEmpty()) {
                throw new IllegalStateException(
                        "No meter change reasons configured. Add meter_change_reason_1, meter_change_reason_2... or language-specific keys.");
            }
            String selectedReason = resolveLanguageSelection(request.getReason(), reasons)
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

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(request.getContactId())
                    .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + request.getContactId()));

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            Long schemeId = telemetryTenantRepository
                    .findFirstSchemeForUser(operatorWithSchema.schemaName(), operatorWithSchema.operator().id())
                    .orElseThrow(() -> new IllegalStateException("Operator is not mapped to any scheme"));

            com.example.telemetry.repository.TelemetryReadingRecord pending;
            if (request.getCorrelationId() != null && !request.getCorrelationId().isBlank()) {
                pending = telemetryTenantRepository
                        .findPendingMeterChangeRecordByCorrelation(
                                operatorWithSchema.schemaName(),
                                schemeId,
                                operatorWithSchema.operator().id(),
                                request.getCorrelationId().trim()
                        )
                        .orElseThrow(() -> new IllegalStateException("Invalid or expired correlationId"));
            } else {
                pending = telemetryTenantRepository
                        .findLatestPendingMeterChangeRecord(
                                operatorWithSchema.schemaName(),
                                schemeId,
                                operatorWithSchema.operator().id()
                        )
                        .orElseThrow(() -> new IllegalStateException("No pending meter-change session found"));
            }

            telemetryTenantRepository.updatePendingMeterChangeReading(
                    operatorWithSchema.schemaName(),
                    pending.id(),
                    manualReadingValue,
                    operatorWithSchema.operator().id()
            );
            CreateReadingResponse response = CreateReadingResponse.builder()
                    .success(true)
                    .correlationId(pending.correlationId())
                    .meterReading(manualReadingValue)
                    .qualityStatus("CONFIRMED")
                    .build();

            String selectedLanguage = resolveOperatorLanguage(operatorWithSchema, tenantId);
            String languageKey = normalizeLanguageKey(selectedLanguage);

            String template = tenantConfigRepository.findManualReadingConfirmationTemplate(tenantId, languageKey)
                    .orElse("Manual reading {reading} saved successfully.");
            response.setMessage(template.replace("{reading}", manualReadingValue.stripTrailingZeros().toPlainString()));

            if (response.getCorrelationId() == null || response.getCorrelationId().isBlank()) {
                response.setCorrelationId(UUID.randomUUID().toString());
            }
            return response;
        } catch (Exception e) {
            log.error("Error processing manual reading for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return CreateReadingResponse.builder()
                    .success(false)
                    .message("Manual reading could not be saved.")
                    .qualityStatus("REJECTED")
                    .correlationId(request.getContactId())
                    .build();
        }
    }

    private String resolveOperatorLanguage(TelemetryOperatorWithSchema operatorWithSchema, Integer tenantId) {
        if (operatorWithSchema == null || operatorWithSchema.operator() == null) {
            return "English";
        }
        Integer languageId = operatorWithSchema.operator().languageId();
        if (languageId == null || languageId <= 0) {
            return "English";
        }
        List<String> languageOptions = tenantConfigRepository.findLanguageOptions(tenantId);
        if (languageOptions.isEmpty() || languageId > languageOptions.size()) {
            return "English";
        }
        return languageOptions.get(languageId - 1);
    }

    private Optional<String> resolveLanguageSelection(String rawSelection, List<String> options) {
        String value = rawSelection.trim();
        if (value.matches("^\\d+$")) {
            int index = Integer.parseInt(value);
            if (index >= 1 && index <= options.size()) {
                return Optional.of(options.get(index - 1));
            }
        }
        if (value.matches("^\\d+\\..*$")) {
            String numeric = value.substring(0, value.indexOf('.')).trim();
            if (numeric.matches("^\\d+$")) {
                int index = Integer.parseInt(numeric);
                if (index >= 1 && index <= options.size()) {
                    return Optional.of(options.get(index - 1));
                }
            }
        }
        return options.stream().filter(v -> v.equalsIgnoreCase(value)).findFirst();
    }

    private String normalizeLanguageKey(String language) {
        if (language == null) {
            return "";
        }

        String raw = language.trim();
        String lower = raw.toLowerCase();

        // Support localized labels stored in DB/user preference.
        if ("हिंदी".equals(raw) || "हिन्दी".equals(raw) || "hindi".equals(lower)) {
            return "hindi";
        }
        if ("english".equals(lower) || "inglish".equals(lower)) {
            return "english";
        }

        return lower.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private String toItemCode(String selectedItemLabel, List<String> itemOptions) {
        int index = -1;
        for (int i = 0; i < itemOptions.size(); i++) {
            if (itemOptions.get(i).equalsIgnoreCase(selectedItemLabel)) {
                index = i + 1;
                break;
            }
        }
        return switch (index) {
            case 1 -> "readingSubmission";
            case 2 -> "channelChange";
            case 3 -> "meterChange";
            case 4 -> "languageChange";
            default -> normalizeLanguageKey(selectedItemLabel);
        };
    }

    private byte[] downloadImageFromGlific(String mediaId) throws IOException {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (glificApiToken != null && !glificApiToken.isBlank()) {
                headers.setBearerAuth(glificApiToken);
            }
            headers.set(HttpHeaders.USER_AGENT, "WaterSupplyBot/1.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    "https://api.glific.org/v1/media/" + mediaId,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new IOException("Failed to download image from Glific, status: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException e) {
            throw new IOException("Failed to download image from Glific: " + e.getMessage(), e);
        }
    }

    private byte[] downloadImage(String url) throws IOException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "WaterSupplyBot/1.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new IOException("Failed to download image, status: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException e) {
            throw new IOException("Failed to download image: " + e.getMessage(), e);
        }
    }
}
