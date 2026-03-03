package com.example.tenant.service;

import com.example.tenant.config.EscalationScheduleConfig;
import com.example.tenant.event.EscalationEvent;
import com.example.tenant.event.OperatorEscalationDetail;
import com.example.tenant.kafka.KafkaProducer;
import com.example.tenant.repository.NudgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes escalations for a single tenant. Called by {@link TenantSchedulerManager}
 * on each tenant's individual schedule.
 *
 * <p>Users who missed &ge; level2Threshold days are escalated to the district
 * officer; those between level1 and level2 thresholds go to the section officer.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EscalationSchedulerService {

    private static final String COMMON_TOPIC = "common-topic";

    private final NudgeRepository nudgeRepository;
    private final TenantConfigService tenantConfigService;
    private final KafkaProducer kafkaProducer;

    public void processEscalationsForTenant(String schema, int tenantId) {
        EscalationScheduleConfig cfg = tenantConfigService.getEscalationConfig(tenantId);
        int level1Days = cfg.getLevel1Days();
        int level2Days = cfg.getLevel2Days();
        String level1UserType = cfg.getLevel1OfficerType();
        String level2UserType = cfg.getLevel2OfficerType();

        log.info("[EscalationJob] schema={} – L1: {} days ({}), L2: {} days ({})",
                schema, level1Days, level1UserType, level2Days, level2UserType);

        List<Map<String, Object>> rows = nudgeRepository.findUsersWithMissedDays(schema, level1Days);
        log.info("[EscalationJob] schema={} → {} users exceeded level1 threshold", schema, rows.size());

        // officer-key → (officerRow, level, detailList)
        // Key = "LEVEL_<n>|<phone>" to keep level1 and level2 officers separate
        Map<String, OfficerGroup> officerGroups = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            // days_since_last_upload is NULL when the operator has never uploaded
            Number daysSinceObj = (Number) row.get("days_since_last_upload");
            boolean neverUploaded = (daysSinceObj == null);
            int daysSinceLastUpload = neverUploaded ? Integer.MAX_VALUE : daysSinceObj.intValue();

            // Never-uploaded operators go straight to level-2 (most severe)
            int escalationLevel = (neverUploaded || daysSinceLastUpload >= level2Days) ? 2 : 1;
            String officerUserType = escalationLevel == 2 ? level2UserType : level1UserType;
            Object schemeId = row.get("scheme_id");

            Map<String, Object> officerRow =
                    nudgeRepository.findOfficerByUserType(schema, schemeId, officerUserType);
            if (officerRow == null) {
                log.debug("[EscalationJob] No {} found for scheme={}, skipping",
                        officerUserType, schemeId);
                continue;
            }

            String officerPhone = (String) officerRow.get("phone_number");
            String officerName = (String) officerRow.get("name");
            int officerLanguageId = officerRow.get("language_id") != null
                    ? ((Number) officerRow.get("language_id")).intValue() : 0;
            if (officerPhone == null || officerPhone.isBlank()) {
                continue;
            }

            // Always look up SO name for informational purposes in the detail
            String soName = "";
            if (escalationLevel == 2) {
                Map<String, Object> soRow =
                        nudgeRepository.findOfficerByUserType(schema, schemeId, level1UserType);
                if (soRow != null) {
                    soName = (String) soRow.getOrDefault("name", "");
                }
            } else {
                soName = officerName; // level1 officer IS the SO
            }

            // Display "Never" when no reading exists; otherwise show the actual date
            Object lastReadingDateObj = row.get("last_reading_date");
            String lastRecordedBfmDate = (lastReadingDateObj == null) ? "Never" : lastReadingDateObj.toString();

            OperatorEscalationDetail detail = OperatorEscalationDetail.builder()
                    .name((String) row.get("name"))
                    .phoneNumber((String) row.get("phone_number"))
                    .schemeName((String) row.getOrDefault("scheme_name", String.valueOf(schemeId)))
                    .schemeId(String.valueOf(schemeId))
                    .soName(soName)
                    .consecutiveDaysMissed(daysSinceLastUpload)
                    .lastRecordedBfmDate(lastRecordedBfmDate)
                    .build();

            String groupKey = "LEVEL_" + escalationLevel + "|" + officerPhone;
            officerGroups.computeIfAbsent(groupKey, k ->
                    new OfficerGroup(officerPhone, officerName, escalationLevel, officerLanguageId))
                    .details.add(detail);
        }

        // Publish one EscalationEvent per officer
        for (OfficerGroup group : officerGroups.values()) {
            EscalationEvent event = EscalationEvent.builder()
                    .eventType("ESCALATION")
                    .escalationLevel(group.level)
                    .officerPhone(group.officerPhone)
                    .officerName(group.officerName)
                    .operators(group.details)
                    .tenantId(tenantId)
                    .officerLanguageId(group.officerLanguageId)
                    .build();
            kafkaProducer.publishJson(COMMON_TOPIC, event);
            log.info("[EscalationJob] Published EscalationEvent level={} with {} operators",
                    group.level, group.details.size());
        }
    }

    private static class OfficerGroup {
        final String officerPhone;
        final String officerName;
        final int level;
        final int officerLanguageId;
        final List<OperatorEscalationDetail> details = new ArrayList<>();

        OfficerGroup(String officerPhone, String officerName, int level, int officerLanguageId) {
            this.officerPhone = officerPhone;
            this.officerName = officerName;
            this.level = level;
            this.officerLanguageId = officerLanguageId;
        }
    }
}
