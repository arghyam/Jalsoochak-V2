package org.arghyam.jalsoochak.telemetry.service;

import org.arghyam.jalsoochak.telemetry.dto.response.ClosingResponse;
import org.arghyam.jalsoochak.telemetry.dto.response.CreateReadingResponse;
import org.arghyam.jalsoochak.telemetry.dto.response.IntroResponse;
import org.arghyam.jalsoochak.telemetry.dto.requests.ClosingRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.CreateReadingRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.GlificWebhookRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.IntroRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.IssueReportRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.ManualReadingRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.MeterChangeRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.SelectedChannelRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.SelectedItemRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.SelectedLanguageRequest;
import org.arghyam.jalsoochak.telemetry.repository.TenantConfigRepository;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryConfirmedReadingSnapshot;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryOperatorWithSchema;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryPendingMeterChangeRecord;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryTenantRepository;
import org.arghyam.jalsoochak.telemetry.repository.UserLanguagePreferenceRepository;
import org.arghyam.jalsoochak.telemetry.dto.response.SelectionResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
    private final UserLanguagePreferenceRepository userLanguagePreferenceRepository;

    private final String glificApiToken;
    private final String glificMediaBaseUrl;
    private final int mediaDownloadRetryMaxAttempts;
    private final long mediaDownloadRetryInitialBackoffMs;

    public GlificWebhookService(MinioService minioService,
                                RestTemplate restTemplate,
                                BfmReadingService bfmReadingService,
                                TelemetryTenantRepository telemetryTenantRepository,
                                TenantConfigRepository tenantConfigRepository,
                                UserLanguagePreferenceRepository userLanguagePreferenceRepository,
                                @Value("${glific.media-base-url:https://api.glific.org/v1/media}") String glificMediaBaseUrl,
                                @Value("${media-download.retry.max-attempts:3}") int mediaDownloadRetryMaxAttempts,
                                @Value("${media-download.retry.initial-backoff-ms:300}") long mediaDownloadRetryInitialBackoffMs,
                                @Value("${glific.api-token:}") String glificApiToken) {
        this.minioService = minioService;
        this.restTemplate = restTemplate;
        this.bfmReadingService = bfmReadingService;
        this.telemetryTenantRepository = telemetryTenantRepository;
        this.tenantConfigRepository = tenantConfigRepository;
        this.userLanguagePreferenceRepository = userLanguagePreferenceRepository;
        this.glificMediaBaseUrl = glificMediaBaseUrl.endsWith("/")
                ? glificMediaBaseUrl.substring(0, glificMediaBaseUrl.length() - 1)
                : glificMediaBaseUrl;
        this.mediaDownloadRetryMaxAttempts = Math.max(1, mediaDownloadRetryMaxAttempts);
        this.mediaDownloadRetryInitialBackoffMs = Math.max(0L, mediaDownloadRetryInitialBackoffMs);
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

            log.debug("Downloaded image for contactId {} (bytes={})", contactId, imageBytes.length);

            String objectKey = "bfm/" + contactId + "/" + System.currentTimeMillis() + ".jpg";
            String imageStorageUrl = minioService.upload(imageBytes, objectKey);
            log.info("imageStorageUrl: {}", imageStorageUrl);
            log.debug("Image uploaded for contactId {} with objectKey {}", contactId, objectKey);

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(contactId);
            String languageKey = normalizeLanguageKey(
                    resolveOperatorLanguage(operatorWithSchema, operatorWithSchema.operator().tenantId())
            );

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

            CreateReadingResponse response = bfmReadingService.createReading(
                    createReadingRequest,
                    operatorWithSchema.schemaName(),
                    operatorWithSchema.operator(),
                    contactId
            );
            response.setMessage(localizeMessage(response.getMessage(), languageKey));
            return response;
        } catch (Exception e) {
            log.error("Unexpected error processing image for contactId {}: {}", glificWebhookRequest.getContactId(), e.getMessage(), e);
            String languageKey = resolveLanguageKeyForContact(glificWebhookRequest.getContactId());
            String descriptiveMessage = resolveUserFacingErrorMessage(e, "Image could not be processed.", languageKey);
            return CreateReadingResponse.builder()
                    .success(false)
                    .message(descriptiveMessage)
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

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(introRequest.getContactId());

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

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(closingRequest.getContactId());

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

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(request.getContactId());

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }
            String languageKey = normalizeLanguageKey(resolveOperatorLanguage(operatorWithSchema, tenantId));

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
                    .correlationId(toWords(languageOptions.size()))
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

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(request.getContactId());

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
            userLanguagePreferenceRepository.upsert(tenantId, request.getContactId(), selectedLanguage);

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
            String languageKey = resolveLanguageKeyForContact(request.getContactId());
            return IntroResponse.builder()
                    .success(false)
                    .message(resolveUserFacingErrorMessage(e, "Language selection could not be saved.", languageKey))
                    .build();
        }
    }

    public IntroResponse channelSelectionMessage(IntroRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(request.getContactId());

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

            int correlationCount = channelOptions.size();
            boolean hasBfmOrElectric = channelOptions.stream().anyMatch(option -> isBfmChannel(option) || isElectricChannel(option));
            String correlationWord = toWords(correlationCount);
            String isBfmOrIsElectricValue = buildBfmOrElectricCorrelationFlag(hasBfmOrElectric, correlationWord);

            return IntroResponse.builder()
                    .success(true)
                    .message(message.toString())
                    .correlationId(correlationWord)
                    .isBfmOrIsElectric(isBfmOrIsElectricValue)
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

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(request.getContactId());

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

            boolean isBfm = isBfmChannel(selectedChannel);
            boolean isElectrical = isElectricChannel(selectedChannel);
            boolean isBfmorIsElectrical = isBfm || isElectrical;

            return IntroResponse.builder()
                    .success(true)
                    .message(confirmationTemplate.replace("{channel}", selectedChannel))
                    .isBfmorIsElectrical(isBfmorIsElectrical)
                    .isBfm(isBfm)
                    .isElectrical(isElectrical)
                    .build();
        } catch (Exception e) {
            log.error("Error saving selected channel for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            String languageKey = resolveLanguageKeyForContact(request.getContactId());
            return IntroResponse.builder()
                    .success(false)
                    .message(resolveUserFacingErrorMessage(e, "Channel selection could not be saved.", languageKey))
                    .build();
        }
    }

    public IntroResponse itemSelectionMessage(IntroRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(request.getContactId());

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
            List<String> visibleItemOptions = applyItemVisibilityRules(operatorWithSchema, tenantId, languageKey, itemOptions);

            StringBuilder message = new StringBuilder(prompt.trim());
            for (int i = 0; i < visibleItemOptions.size(); i++) {
                message.append("\n")
                        .append(i + 1)
                        .append(". ")
                        .append(visibleItemOptions.get(i));
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

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(request.getContactId());

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
            List<String> visibleItemOptions = applyItemVisibilityRules(operatorWithSchema, tenantId, languageKey, itemOptions);

            String selectedItemLabel = resolveLanguageSelection(request.getChannel(), visibleItemOptions)
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
            String languageKey = resolveLanguageKeyForContact(request.getContactId());
            return SelectionResponse.builder()
                    .success(false)
                    .selected(null)
                    .message(resolveUserFacingErrorMessage(e, "Item selection could not be saved.", languageKey))
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

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(request.getContactId());

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

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(request.getContactId());

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

    public IntroResponse issueReportPromptMessage(IntroRequest request) {
        try {
            if (request.getContactId() == null || request.getContactId().isBlank()) {
                throw new IllegalStateException("contactId is required");
            }

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(request.getContactId());

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String selectedLanguage = resolveOperatorLanguage(operatorWithSchema, tenantId);
            String languageKey = normalizeLanguageKey(selectedLanguage);

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

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(request.getContactId());

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

            String selectedLanguage = resolveOperatorLanguage(operatorWithSchema, tenantId);
            String languageKey = normalizeLanguageKey(selectedLanguage);

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

            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(request.getContactId());

            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }
            String languageKey = normalizeLanguageKey(resolveOperatorLanguage(operatorWithSchema, tenantId));

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
                        .message(localizeMessage("Manual reading cannot be less than previous reading.", languageKey))
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
            String languageKey = resolveLanguageKeyForContact(request.getContactId());
            String descriptiveMessage = resolveUserFacingErrorMessage(e, "Manual reading could not be saved.", languageKey);
            return CreateReadingResponse.builder()
                    .success(false)
                    .message(descriptiveMessage)
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

    private String toWords(int number) {
        if (number == 0) {
            return "zero";
        }
        return toWordsInternal(number).trim();
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

    private String resolveUserFacingErrorMessage(Exception e, String fallback, String languageKey) {
        if (e == null) {
            return localizeMessage(fallback, languageKey);
        }
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return localizeMessage(fallback, languageKey);
        }

        String normalized = message.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("duplicate image")) {
            return localizeMessage("Duplicate image submission detected. Please submit a new image.", languageKey);
        }
        if (normalized.contains("less than previous")) {
            return localizeMessage("Reading cannot be less than previous confirmed reading.", languageKey);
        }
        if (normalized.contains("manualreading is required")) {
            return localizeMessage("manualReading is required.", languageKey);
        }
        if (normalized.contains("manualreading must be numeric")) {
            return localizeMessage("manualReading must be a numeric value.", languageKey);
        }
        if (normalized.contains("manualreading must be greater than zero")) {
            return localizeMessage("manualReading must be greater than zero.", languageKey);
        }
        if (normalized.contains("language selection is required")) {
            return localizeMessage("Language selection is required. Please choose one of the listed options.", languageKey);
        }
        if (normalized.contains("invalid language selection")) {
            return localizeMessage("Invalid language selection. Please choose a valid number or language from the list.", languageKey);
        }
        if (normalized.contains("no language options configured")) {
            return localizeMessage("Language options are not configured for this tenant.", languageKey);
        }
        if (normalized.contains("channel selection is required")) {
            return localizeMessage("Channel selection is required. Please choose one of the listed options.", languageKey);
        }
        if (normalized.contains("invalid channel selection")) {
            return localizeMessage("Invalid channel selection. Please choose a valid number or channel from the list.", languageKey);
        }
        if (normalized.contains("no channel options configured")) {
            return localizeMessage("Channel options are not configured for this tenant.", languageKey);
        }
        if (normalized.contains("item selection is required")) {
            return localizeMessage("Item selection is required. Please choose one of the listed options.", languageKey);
        }
        if (normalized.contains("invalid item selection")) {
            return localizeMessage("Invalid item selection. Please choose a valid option from the list.", languageKey);
        }
        if (normalized.contains("no item options configured")) {
            return localizeMessage("Item options are not configured for this tenant.", languageKey);
        }
        if (normalized.contains("operator is not mapped to any scheme")) {
            return localizeMessage("No scheme is mapped to this operator.", languageKey);
        }
        if (normalized.contains("operator could not be resolved")) {
            return localizeMessage("Operator could not be resolved for this contact.", languageKey);
        }
        if (normalized.contains("invalid media")) {
            return localizeMessage("Invalid media. Please submit a clear meter image.", languageKey);
        }
        return localizeMessage(message.trim(), languageKey);
    }

    private String resolveLanguageKeyForContact(String contactId) {
        try {
            if (contactId == null || contactId.isBlank()) {
                return "english";
            }
            TelemetryOperatorWithSchema operatorWithSchema = resolveOperatorWithSchema(contactId);
            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                return "english";
            }
            return normalizeLanguageKey(resolveOperatorLanguage(operatorWithSchema, tenantId));
        } catch (Exception ignored) {
            return "english";
        }
    }

    private String localizeMessage(String message, String languageKey) {
        if (message == null || message.isBlank()) {
            return message;
        }
        if (!"hindi".equals(languageKey)) {
            return message;
        }

        String normalized = message.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("duplicate image submission detected")) {
            return "डुप्लिकेट इमेज मिली है। कृपया नई इमेज सबमिट करें।";
        }
        if (normalized.contains("reading cannot be less than previous")) {
            return "रीडिंग पिछली पुष्टि की गई रीडिंग से कम नहीं हो सकती।";
        }
        if (normalized.contains("manual reading cannot be less than previous")) {
            return "मैनुअल रीडिंग पिछली पुष्टि की गई रीडिंग से कम नहीं हो सकती।";
        }
        if (normalized.contains("manualreading is required")) {
            return "manualReading अनिवार्य है।";
        }
        if (normalized.contains("manualreading must be a numeric value")) {
            return "manualReading केवल संख्या होना चाहिए।";
        }
        if (normalized.contains("manualreading must be greater than zero")) {
            return "manualReading शून्य से बड़ा होना चाहिए।";
        }
        if (normalized.contains("language selection is required")) {
            return "भाषा चयन आवश्यक है। कृपया सूची में से एक विकल्प चुनें।";
        }
        if (normalized.contains("invalid language selection")) {
            return "अमान्य भाषा चयन। कृपया सूची से सही संख्या या भाषा चुनें।";
        }
        if (normalized.contains("language options are not configured")) {
            return "इस टेनेंट के लिए भाषा विकल्प कॉन्फ़िगर नहीं हैं।";
        }
        if (normalized.contains("channel selection is required")) {
            return "चैनल चयन आवश्यक है। कृपया सूची में से एक विकल्प चुनें।";
        }
        if (normalized.contains("invalid channel selection")) {
            return "अमान्य चैनल चयन। कृपया सूची से सही संख्या या चैनल चुनें।";
        }
        if (normalized.contains("channel options are not configured")) {
            return "इस टेनेंट के लिए चैनल विकल्प कॉन्फ़िगर नहीं हैं।";
        }
        if (normalized.contains("item selection is required")) {
            return "विकल्प चयन आवश्यक है। कृपया सूची में से एक विकल्प चुनें।";
        }
        if (normalized.contains("invalid item selection")) {
            return "अमान्य विकल्प चयन। कृपया सूची से सही विकल्प चुनें।";
        }
        if (normalized.contains("item options are not configured")) {
            return "इस टेनेंट के लिए विकल्प कॉन्फ़िगर नहीं हैं।";
        }
        if (normalized.contains("no scheme is mapped to this operator")) {
            return "इस ऑपरेटर के लिए कोई स्कीम मैप नहीं है।";
        }
        if (normalized.contains("operator could not be resolved")) {
            return "इस संपर्क के लिए ऑपरेटर नहीं मिला।";
        }
        if (normalized.contains("invalid media")) {
            return "मीडिया अमान्य है। कृपया स्पष्ट मीटर इमेज भेजें।";
        }
        if (normalized.contains("image could not be processed")) {
            return "इमेज प्रोसेस नहीं हो सकी। कृपया दोबारा प्रयास करें।";
        }
        if (normalized.contains("manual reading could not be saved")) {
            return "मैनुअल रीडिंग सेव नहीं हो सकी। कृपया दोबारा प्रयास करें।";
        }
        if (normalized.contains("could not read meter value from image")) {
            return "इमेज से मीटर रीडिंग नहीं पढ़ी जा सकी। कृपया स्पष्ट फोटो भेजें।";
        }
        if (normalized.contains("ocr failed")) {
            return "मीटर रीडिंग पढ़ने में त्रुटि हुई। कृपया स्पष्ट फोटो भेजें।";
        }
        return message;
    }

    private String toWordsInternal(int number) {
        if (number < 0) {
            return "minus " + toWordsInternal(-number);
        }
        if (number < 20) {
            return SMALL_NUMBERS[number];
        }
        if (number < 100) {
            String tensWord = TENS[(number / 10)];
            int remainder = number % 10;
            if (remainder == 0) {
                return tensWord;
            }
            return tensWord + " " + SMALL_NUMBERS[remainder];
        }
        if (number < 1_000) {
            int remainder = number % 100;
            String result = SMALL_NUMBERS[number / 100] + " hundred";
            if (remainder == 0) {
                return result;
            }
            return result + " " + toWordsInternal(remainder);
        }
        if (number < 1_000_000) {
            return withRemainder(number, 1_000, "thousand");
        }
        if (number < 1_000_000_000) {
            return withRemainder(number, 1_000_000, "million");
        }
        return withRemainder(number, 1_000_000_000, "billion");
    }

    private String withRemainder(int number, int divisor, String unit) {
        int remainder = number % divisor;
        String result = toWordsInternal(number / divisor) + " " + unit;
        if (remainder == 0) {
            return result;
        }
        return result + " " + toWordsInternal(remainder);
    }

    private String buildBfmOrElectricCorrelationFlag(boolean hasBfmOrElectric, String correlationWord) {
        if (correlationWord == null || correlationWord.isBlank()) {
            return null;
        }
        String normalized = correlationWord.trim();
        String capitalized = normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
        if (hasBfmOrElectric) {
            return "bfmOrElectricpresentandcorrelationIs" + capitalized;
        }
        return "bfmOrElectricNotpresentandcorrelationIs" + capitalized;
    }

    private static final String[] SMALL_NUMBERS = {
            "zero",
            "one",
            "two",
            "three",
            "four",
            "five",
            "six",
            "seven",
            "eight",
            "nine",
            "ten",
            "eleven",
            "twelve",
            "thirteen",
            "fourteen",
            "fifteen",
            "sixteen",
            "seventeen",
            "eighteen",
            "nineteen"
    };

    private static final String[] TENS = {
            "",
            "",
            "twenty",
            "thirty",
            "forty",
            "fifty",
            "sixty",
            "seventy",
            "eighty",
            "ninety"
    };

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
            case 3 -> "reportIssue";
            case 4 -> "languageChange";
            default -> normalizeLanguageKey(selectedItemLabel);
        };
    }

    private List<String> applyItemVisibilityRules(TelemetryOperatorWithSchema operatorWithSchema,
                                                  Integer tenantId,
                                                  String languageKey,
                                                  List<String> itemOptions) {
        List<String> channelOptions = tenantConfigRepository.findChannelOptions(tenantId, languageKey);
        boolean showChannelChange = channelOptions.size() > 1;

        List<String> languageOptions = tenantConfigRepository.findLanguageOptions(tenantId);
        boolean showLanguageChange = languageOptions.size() > 1;

        List<String> filtered = new java.util.ArrayList<>();
        for (String option : itemOptions) {
            String itemCode = toItemCode(option, itemOptions);
            if ("channelChange".equals(itemCode) && !showChannelChange) {
                continue;
            }
            if ("languageChange".equals(itemCode) && !showLanguageChange) {
                continue;
            }
            if ("reportIssue".equals(itemCode)) {
                filtered.add(normalizeIssueReportLabel(option, languageKey));
                continue;
            }
            filtered.add(option);
        }
        return filtered.isEmpty() ? itemOptions : filtered;
    }

    private String normalizeIssueReportLabel(String option, String languageKey) {
        if (option == null) {
            return option;
        }
        String trimmed = option.trim();
        if ("hindi".equals(languageKey)) {
            if ("मीटर परिवर्तन".equalsIgnoreCase(trimmed)) {
                return "समस्या रिपोर्ट करें";
            }
            return option;
        }
        if ("meter change".equalsIgnoreCase(trimmed)) {
            return "Report Issue";
        }
        return option;
    }

    private boolean isBfmChannel(String channelOption) {
        if (channelOption == null) {
            return false;
        }
        String normalized = channelOption.toLowerCase().replaceAll("[^a-z0-9]+", "");
        return normalized.contains("bfm");
    }

    private boolean isElectricChannel(String channelOption) {
        if (channelOption == null) {
            return false;
        }
        String normalized = channelOption.toLowerCase().replaceAll("[^a-z0-9]+", "");
        return normalized.contains("electric");
    }

    private TelemetryOperatorWithSchema resolveOperatorWithSchema(String contactId) {
        Integer preferredTenantId = userLanguagePreferenceRepository
                .findPreferredTenantIdByContactId(contactId)
                .orElse(null);

        return telemetryTenantRepository
                .findOperatorByPhoneAcrossTenants(contactId, preferredTenantId)
                .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + contactId));
    }

    private byte[] downloadImageFromGlific(String mediaId) throws IOException {
        for (int attempt = 1; attempt <= mediaDownloadRetryMaxAttempts; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                if (glificApiToken != null && !glificApiToken.isBlank()) {
                    headers.setBearerAuth(glificApiToken);
                }
                headers.set(HttpHeaders.USER_AGENT, "WaterSupplyBot/1.0");
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> response = restTemplate.exchange(
                        glificMediaBaseUrl + "/" + mediaId,
                        HttpMethod.GET,
                        entity,
                        byte[].class
                );

                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                    throw new IOException("Failed to download image from Glific, status: " + response.getStatusCode());
                }
                return response.getBody();
            } catch (RestClientException e) {
                if (attempt == mediaDownloadRetryMaxAttempts) {
                    throw new IOException("Failed to download image from Glific after " + attempt + " attempts: " + e.getMessage(), e);
                }
                long backoffMs = mediaDownloadRetryInitialBackoffMs * (1L << (attempt - 1));
                log.warn("Glific media download attempt {} failed for mediaId {}. Retrying in {} ms", attempt, mediaId, backoffMs);
                sleepBackoff(backoffMs);
            }
        }
        throw new IOException("Failed to download image from Glific");
    }

    private byte[] downloadImage(String url) throws IOException {
        for (int attempt = 1; attempt <= mediaDownloadRetryMaxAttempts; attempt++) {
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
                if (attempt == mediaDownloadRetryMaxAttempts) {
                    throw new IOException("Failed to download image after " + attempt + " attempts: " + e.getMessage(), e);
                }
                long backoffMs = mediaDownloadRetryInitialBackoffMs * (1L << (attempt - 1));
                log.warn("Media download attempt {} failed for URL {}. Retrying in {} ms", attempt, url, backoffMs);
                sleepBackoff(backoffMs);
            }
        }
        throw new IOException("Failed to download image");
    }

    private void sleepBackoff(long backoffMs) {
        if (backoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
