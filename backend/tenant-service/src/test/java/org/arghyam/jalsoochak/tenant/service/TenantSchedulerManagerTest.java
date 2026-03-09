package org.arghyam.jalsoochak.tenant.service;

import org.arghyam.jalsoochak.tenant.config.EscalationScheduleConfig;
import org.arghyam.jalsoochak.tenant.config.NudgeScheduleConfig;
import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TenantSchedulerManager} scheduling logic.
 */
@ExtendWith(MockitoExtension.class)
class TenantSchedulerManagerTest {

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private TenantCommonRepository tenantCommonRepository;

    @Mock
    private TenantConfigService tenantConfigService;

    @Mock
    private NudgeSchedulerService nudgeSchedulerService;

    @Mock
    private EscalationSchedulerService escalationSchedulerService;

    @InjectMocks
    private TenantSchedulerManager manager;

    @SuppressWarnings("rawtypes")
    @Mock
    private ScheduledFuture future;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
                .thenReturn(future);
    }

    // ── loadAndScheduleAll ──────────────────────────────────────────────────────

    @Test
    void loadAndScheduleAll_schedulesNudgeAndEscalation_forEachActiveTenant() {
        TenantResponseDTO t1 = TenantResponseDTO.builder().id(1).stateCode("MP").status("ACTIVE").build();
        TenantResponseDTO t2 = TenantResponseDTO.builder().id(2).stateCode("UP").status("ACTIVE").build();
        when(tenantCommonRepository.findAll()).thenReturn(List.of(t1, t2));

        stubConfigs(1, 8, 0, 9, 0);
        stubConfigs(2, 8, 0, 9, 0);

        manager.loadAndScheduleAll();

        // 2 nudge + 2 escalation = 4 schedule calls
        verify(taskScheduler, times(4)).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void loadAndScheduleAll_skipsInactiveTenants() {
        TenantResponseDTO active = TenantResponseDTO.builder().id(1).stateCode("MP").status("ACTIVE").build();
        TenantResponseDTO inactive = TenantResponseDTO.builder().id(2).stateCode("UP").status("INACTIVE").build();
        when(tenantCommonRepository.findAll()).thenReturn(List.of(active, inactive));

        stubConfigs(1, 8, 0, 9, 0);

        manager.loadAndScheduleAll();

        // Only 2 calls for the active tenant
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void loadAndScheduleAll_buildsCronExpression_fromConfigHourAndMinute() {
        TenantResponseDTO t1 = TenantResponseDTO.builder().id(1).stateCode("MP").status("ACTIVE").build();
        when(tenantCommonRepository.findAll()).thenReturn(List.of(t1));

        stubConfigs(1, 10, 30, 11, 15);

        manager.loadAndScheduleAll();

        ArgumentCaptor<CronTrigger> triggerCaptor = ArgumentCaptor.forClass(CronTrigger.class);
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), triggerCaptor.capture());

        List<CronTrigger> triggers = triggerCaptor.getAllValues();
        // nudge cron: 0 30 10 * * ?
        assertThat(triggers.get(0).toString()).contains("0 30 10");
        // escalation cron: 0 15 11 * * ?
        assertThat(triggers.get(1).toString()).contains("0 15 11");
    }

    // ── rescheduleForTenant ─────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void rescheduleForTenant_cancelsOldFuturesBeforeSchedulingNew() {
        // First schedule
        TenantResponseDTO t1 = TenantResponseDTO.builder().id(1).stateCode("MP").status("ACTIVE").build();
        when(tenantCommonRepository.findAll()).thenReturn(List.of(t1));
        stubConfigs(1, 8, 0, 9, 0);
        manager.loadAndScheduleAll();

        // Now reschedule with new config
        stubConfigs(1, 10, 30, 11, 0);
        manager.rescheduleForTenant(1, "MP");

        // Old futures should have been cancelled (2 from initial schedule)
        verify(future, times(2)).cancel(false);
        // And 2 new futures scheduled (total 4 schedule calls: 2 initial + 2 new)
        verify(taskScheduler, times(4)).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void rescheduleForTenant_doesNotCancelNonExistentFutures_whenNoneScheduled() {
        // reschedule without prior loadAndScheduleAll
        stubConfigs(99, 8, 0, 9, 0);

        manager.rescheduleForTenant(99, "GJ");

        // No cancel calls – no existing futures
        verify(future, never()).cancel(anyBoolean());
        // But 2 new futures should be scheduled
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private void stubConfigs(int tenantId, int nudgeHour, int nudgeMin, int escalHour, int escalMin) {
        when(tenantConfigService.getNudgeConfig(tenantId))
                .thenReturn(NudgeScheduleConfig.builder().hour(nudgeHour).minute(nudgeMin).build());
        when(tenantConfigService.getEscalationConfig(tenantId))
                .thenReturn(EscalationScheduleConfig.builder()
                        .hour(escalHour).minute(escalMin)
                        .level1Days(3).level1OfficerType("SECTION_OFFICER")
                        .level2Days(7).level2OfficerType("DISTRICT_OFFICER")
                        .build());
    }
}
