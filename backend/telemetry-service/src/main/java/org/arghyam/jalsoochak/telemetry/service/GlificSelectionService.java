package org.arghyam.jalsoochak.telemetry.service;

import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.telemetry.dto.requests.IntroRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.SelectedChannelRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.SelectedItemRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.SelectedLanguageRequest;
import org.arghyam.jalsoochak.telemetry.dto.response.IntroResponse;
import org.arghyam.jalsoochak.telemetry.dto.response.SelectionResponse;
import org.arghyam.jalsoochak.telemetry.repository.TenantConfigRepository;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryOperatorWithSchema;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryTenantRepository;
import org.arghyam.jalsoochak.telemetry.repository.UserLanguagePreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
public class GlificSelectionService {

    private static final String[] SMALL_NUMBERS = {
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
    };

    private static final String[] TENS = {
            "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
    };

    private final GlificOperatorContextService operatorContextService;
    private final GlificLocalizationService localizationService;
    private final TenantConfigRepository tenantConfigRepository;
    private final TelemetryTenantRepository telemetryTenantRepository;
    private final UserLanguagePreferenceRepository userLanguagePreferenceRepository;

    public GlificSelectionService(GlificOperatorContextService operatorContextService,
                                  GlificLocalizationService localizationService,
                                  TenantConfigRepository tenantConfigRepository,
                                  TelemetryTenantRepository telemetryTenantRepository,
                                  UserLanguagePreferenceRepository userLanguagePreferenceRepository) {
        this.operatorContextService = operatorContextService;
        this.localizationService = localizationService;
        this.tenantConfigRepository = tenantConfigRepository;
        this.telemetryTenantRepository = telemetryTenantRepository;
        this.userLanguagePreferenceRepository = userLanguagePreferenceRepository;
    }

    public IntroResponse languageSelectionMessage(IntroRequest request) {
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
            String prompt = tenantConfigRepository.findLanguageSelectionPrompt(tenantId, languageKey)
                    .orElseThrow(() -> new IllegalStateException("language_selection_prompt is not configured"));

            List<String> languageOptions = tenantConfigRepository.findLanguageOptions(tenantId);
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

            TelemetryOperatorWithSchema operatorWithSchema = operatorContextService.resolveOperatorWithSchema(request.getContactId());
            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            List<String> languageOptions = tenantConfigRepository.findLanguageOptions(tenantId);
            if (languageOptions.isEmpty()) {
                throw new IllegalStateException("No language options configured for tenant");
            }

            String selectedLanguage = resolveSelection(request.getLanguage(), languageOptions)
                    .orElseThrow(() -> new IllegalStateException("Invalid language selection"));

            int selectedLanguageId = languageOptions.indexOf(selectedLanguage) + 1;
            telemetryTenantRepository.updateUserLanguageId(
                    operatorWithSchema.schemaName(),
                    operatorWithSchema.operator().id(),
                    selectedLanguageId
            );
            userLanguagePreferenceRepository.upsert(tenantId, request.getContactId(), selectedLanguage);

            String languageSpecificKey = "language_selection_confirmation_template_" + localizationService.normalizeLanguageKey(selectedLanguage);
            String confirmationTemplate = tenantConfigRepository
                    .findConfigValue(tenantId, languageSpecificKey)
                    .or(() -> tenantConfigRepository.findConfigValue(tenantId, "language_selection_confirmation_template"))
                    .orElse("Language selected: {language}");
            String selectedLanguageKey = localizationService.normalizeLanguageKey(selectedLanguage);
            String localizedConfirmation = localizationService.localizeMessage(
                    confirmationTemplate.replace("{language}", selectedLanguage),
                    selectedLanguageKey
            );

            return IntroResponse.builder()
                    .success(true)
                    .message(localizedConfirmation)
                    .build();
        } catch (Exception e) {
            log.error("Error saving selected language for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            String languageKey = localizationService.resolveLanguageKeyForContact(request.getContactId());
            return IntroResponse.builder()
                    .success(false)
                    .message(localizationService.resolveUserFacingErrorMessage(e, "Language selection could not be saved.", languageKey))
                    .build();
        }
    }

    public IntroResponse channelSelectionMessage(IntroRequest request) {
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

            TelemetryOperatorWithSchema operatorWithSchema = operatorContextService.resolveOperatorWithSchema(request.getContactId());
            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String languageKey = localizationService.normalizeLanguageKey(operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId));

            List<String> channelOptions = tenantConfigRepository.findChannelOptions(tenantId, languageKey);
            if (channelOptions.isEmpty()) {
                throw new IllegalStateException("No channel options configured for tenant");
            }

            String selectedChannel = resolveSelection(request.getChannel(), channelOptions)
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
            String languageKey = localizationService.resolveLanguageKeyForContact(request.getContactId());
            return IntroResponse.builder()
                    .success(false)
                    .message(localizationService.resolveUserFacingErrorMessage(e, "Channel selection could not be saved.", languageKey))
                    .build();
        }
    }

    public IntroResponse itemSelectionMessage(IntroRequest request) {
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

            String prompt = tenantConfigRepository.findItemSelectionPrompt(tenantId, languageKey)
                    .orElse("Please select what you want to do:");

            List<String> itemOptions = tenantConfigRepository.findItemOptions(tenantId, languageKey);
            if (itemOptions.isEmpty()) {
                throw new IllegalStateException("No item options configured. Add item_1/item_2... or language-specific keys.");
            }
            List<String> visibleItemOptions = applyItemVisibilityRules(tenantId, languageKey, itemOptions);

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

            TelemetryOperatorWithSchema operatorWithSchema = operatorContextService.resolveOperatorWithSchema(request.getContactId());
            Integer tenantId = operatorWithSchema.operator().tenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Operator tenant could not be resolved");
            }

            String languageKey = localizationService.normalizeLanguageKey(operatorContextService.resolveOperatorLanguage(operatorWithSchema, tenantId));

            List<String> itemOptions = tenantConfigRepository.findItemOptions(tenantId, languageKey);
            if (itemOptions.isEmpty()) {
                throw new IllegalStateException("No item options configured for tenant");
            }
            List<String> visibleItemOptions = applyItemVisibilityRules(tenantId, languageKey, itemOptions);

            String selectedItemLabel = resolveSelection(request.getChannel(), visibleItemOptions)
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
            String languageKey = localizationService.resolveLanguageKeyForContact(request.getContactId());
            return SelectionResponse.builder()
                    .success(false)
                    .selected(null)
                    .message(localizationService.resolveUserFacingErrorMessage(e, "Item selection could not be saved.", languageKey))
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

    private String toWords(int number) {
        if (number == 0) {
            return "zero";
        }
        return toWordsInternal(number).trim();
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
            default -> localizationService.normalizeLanguageKey(selectedItemLabel);
        };
    }

    private List<String> applyItemVisibilityRules(Integer tenantId,
                                                  String languageKey,
                                                  List<String> itemOptions) {
        List<String> channelOptions = tenantConfigRepository.findChannelOptions(tenantId, languageKey);
        boolean showChannelChange = channelOptions.size() > 1;

        List<String> languageOptions = tenantConfigRepository.findLanguageOptions(tenantId);
        boolean showLanguageChange = languageOptions.size() > 1;

        List<String> filtered = new ArrayList<>();
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
}
