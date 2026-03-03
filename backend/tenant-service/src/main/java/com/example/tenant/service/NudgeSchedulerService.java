package com.example.tenant.service;

import com.example.tenant.event.NudgeEvent;
import com.example.tenant.kafka.KafkaProducer;
import com.example.tenant.repository.NudgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Processes nudges for a single tenant. Called by {@link TenantSchedulerManager}
 * on each tenant's individual schedule.
 *
 * <p>Sends a WhatsApp nudge to every operator who has not yet submitted a
 * reading for the current day.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NudgeSchedulerService {

    private static final String COMMON_TOPIC = "common-topic";

    private final NudgeRepository nudgeRepository;
    private final KafkaProducer kafkaProducer;

    public void processNudgesForTenant(String schema, int tenantId) {
        List<Map<String, Object>> users = nudgeRepository.findUsersWithNoUploadToday(schema);
        log.info("[NudgeJob] schema={} → {} users have no upload today", schema, users.size());

        for (Map<String, Object> row : users) {
            String phone = (String) row.get("phone_number");
            if (phone == null || phone.isBlank()) {
                continue;
            }
            NudgeEvent event = NudgeEvent.builder()
                    .eventType("NUDGE")
                    .recipientPhone(phone)
                    .operatorName((String) row.get("name"))
                    .schemeId(row.get("scheme_id") != null ? row.get("scheme_id").toString() : "")
                    .tenantId(tenantId)
                    .languageId(row.get("language_id") != null ? ((Number) row.get("language_id")).intValue() : 0)
                    .build();
            kafkaProducer.publishJson(COMMON_TOPIC, event);
            log.debug("[NudgeJob] Published NudgeEvent for phone={}", phone);
        }
    }
}
