package org.arghyam.jalsoochak.tenant.service;

import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.event.NudgeEvent;
import org.arghyam.jalsoochak.tenant.kafka.KafkaProducer;
import org.arghyam.jalsoochak.tenant.repository.NudgeRepository;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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

    private final TenantCommonRepository tenantCommonRepository;
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
                    .userId(row.get("user_id") != null ? ((Number) row.get("user_id")).longValue() : 0L)
                    .whatsappConnectionId(row.get("whatsapp_connection_id") != null
                            ? ((Number) row.get("whatsapp_connection_id")).longValue() : 0L)
                    .tenantSchema(schema)
                    .build();
            kafkaProducer.publishJson(COMMON_TOPIC, event);
            log.debug("[NudgeJob] Published NudgeEvent for phone={}", phone);
        }
    }
}
