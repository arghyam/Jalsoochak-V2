package org.arghyam.jalsoochak.analytics.scheduler.task;

import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class NationalDashboardRefreshTask implements AnalyticsScheduledTask {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private final SchemeRegularityService schemeRegularityService;

    @Value("${analytics.scheduler.national-dashboard.lookback-days:30}")
    private int lookbackDays;

    @Override
    public String taskName() {
        return "national-dashboard-refresh";
    }

    @Override
    @Scheduled(
            cron = "0 0 19 * * *",
            zone = "Asia/Kolkata")
    public void runTask() {
        int sanitizedLookbackDays = Math.max(0, lookbackDays);
        LocalDate endDate = LocalDate.now(IST_ZONE);
        LocalDate startDate = endDate.minusDays(sanitizedLookbackDays);

        log.info("Running scheduled task '{}' for range {} to {}", taskName(), startDate, endDate);
        schemeRegularityService.refreshNationalDashboard(startDate, endDate);
        log.info("Completed scheduled task '{}' for range {} to {}", taskName(), startDate, endDate);
    }
}
