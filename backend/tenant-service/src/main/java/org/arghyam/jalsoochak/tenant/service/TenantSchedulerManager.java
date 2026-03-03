package org.arghyam.jalsoochak.tenant.service;

import org.arghyam.jalsoochak.tenant.config.EscalationScheduleConfig;
import org.arghyam.jalsoochak.tenant.config.NudgeScheduleConfig;
import org.arghyam.jalsoochak.tenant.dto.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Manages per-tenant scheduled jobs for nudges and escalations.
 *
 * <p>At startup, reads all active tenants and schedules a nudge job and an
 * escalation job for each, using the cron times from
 * {@code common_schema.tenant_config_master_table}. Missing config rows fall
 * back to the application.yml defaults.</p>
 *
 * <p>Call {@link #rescheduleForTenant(int, String)} after a tenant's config
 * is updated (write side handled by another engineer) to apply new cron times
 * immediately without a service restart.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantSchedulerManager {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final TenantCommonRepository tenantCommonRepository;
    private final TenantConfigService tenantConfigService;
    private final NudgeSchedulerService nudgeSchedulerService;
    private final EscalationSchedulerService escalationSchedulerService;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadAndScheduleAll() {
        List<TenantResponseDTO> tenants = tenantCommonRepository.findAll();
        tenants.stream()
                .filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
                .forEach(t -> scheduleForTenant(t.getId() != null ? t.getId() : 0, t.getStateCode()));
    }

    /**
     * Called after a tenant's config is persisted to reschedule its jobs with the new cron times.
     */
    public void rescheduleForTenant(int tenantId, String stateCode) {
        cancelFutures(tenantId);
        scheduleForTenant(tenantId, stateCode);
    }

    private void scheduleForTenant(int tenantId, String stateCode) {
        String schema = "tenant_" + stateCode.toLowerCase();

        NudgeScheduleConfig nudgeCfg = tenantConfigService.getNudgeConfig(tenantId);
        EscalationScheduleConfig escalCfg = tenantConfigService.getEscalationConfig(tenantId);

        String nudgeCron = String.format("0 %d %d * * ?", nudgeCfg.getMinute(), nudgeCfg.getHour());
        String escalCron = String.format("0 %d %d * * ?", escalCfg.getMinute(), escalCfg.getHour());

        futures.put("nudge_" + tenantId,
                taskScheduler.schedule(
                        () -> {
                            try {
                                nudgeSchedulerService.processNudgesForTenant(schema, tenantId);
                            } catch (Exception e) {
                                log.error("[Scheduler] Nudge job failed for tenant={}: {}", tenantId, e.getMessage(), e);
                            }
                        },
                        new CronTrigger(nudgeCron)));

        futures.put("escalation_" + tenantId,
                taskScheduler.schedule(
                        () -> {
                            try {
                                escalationSchedulerService.processEscalationsForTenant(schema, tenantId);
                            } catch (Exception e) {
                                log.error("[Scheduler] Escalation job failed for tenant={}: {}", tenantId, e.getMessage(), e);
                            }
                        },
                        new CronTrigger(escalCron)));

        log.info("[Scheduler] Tenant {} ({}): nudge={}, escalation={}", tenantId, stateCode, nudgeCron, escalCron);
    }

    private void cancelFutures(int tenantId) {
        for (String prefix : List.of("nudge_", "escalation_")) {
            ScheduledFuture<?> f = futures.remove(prefix + tenantId);
            if (f != null) f.cancel(false);
        }
    }
}
