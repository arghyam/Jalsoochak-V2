package com.example.tenant.service;

import com.example.tenant.dto.TenantResponseDTO;
import com.example.tenant.event.NudgeEvent;
import com.example.tenant.kafka.KafkaProducer;
import com.example.tenant.repository.NudgeRepository;
import com.example.tenant.repository.TenantCommonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Scheduled job that fires every morning and sends a WhatsApp nudge to every
 * operator who has not yet submitted a reading for the current day.
 *
 * <p>Cron is configurable via {@code nudge.cron} (default 08:00 daily).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NudgeSchedulerService {

    private static final String COMMON_TOPIC = "common-topic";

    private final TenantCommonRepository tenantCommonRepository;
    private final NudgeRepository nudgeRepository;
    private final KafkaProducer kafkaProducer;

    @Scheduled(cron = "${nudge.cron:0 0 8 * * ?}")
    public void runNudgeJob() {
        log.info("[NudgeJob] Starting nudge cron job");
        List<TenantResponseDTO> tenants = tenantCommonRepository.findAll();

        for (TenantResponseDTO tenant : tenants) {
            if (!"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
                continue;
            }
            String schema = "tenant_" + tenant.getStateCode().toLowerCase();
            try {
                processNudgesForTenant(schema);
            } catch (Exception e) {
                log.error("[NudgeJob] Failed for tenant schema '{}': {}", schema, e.getMessage(), e);
            }
        }
        log.info("[NudgeJob] Nudge cron job completed");
    }

    private void processNudgesForTenant(String schema) {
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
                    .schemeId(String.valueOf(row.get("scheme_id")))
                    .build();
            kafkaProducer.publishJson(COMMON_TOPIC, event);
            log.debug("[NudgeJob] Published NudgeEvent for phone={}", phone);
        }
    }
}
