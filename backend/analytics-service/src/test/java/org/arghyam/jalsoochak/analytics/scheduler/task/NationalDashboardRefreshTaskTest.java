package org.arghyam.jalsoochak.analytics.scheduler.task;

import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NationalDashboardRefreshTaskTest {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    @Mock
    private SchemeRegularityService schemeRegularityService;

    @InjectMocks
    private NationalDashboardRefreshTask nationalDashboardRefreshTask;

    @Test
    void runTask_refreshesNationalDashboardForConfiguredWindow() {
        ReflectionTestUtils.setField(nationalDashboardRefreshTask, "lookbackDays", 29);
        LocalDate expectedEndDate = LocalDate.now(IST_ZONE);
        LocalDate expectedStartDate = expectedEndDate.minusDays(29);

        nationalDashboardRefreshTask.runTask();

        verify(schemeRegularityService).refreshNationalDashboard(expectedStartDate, expectedEndDate);
    }

    @Test
    void runTask_negativeLookbackDays_usesSingleDayWindow() {
        ReflectionTestUtils.setField(nationalDashboardRefreshTask, "lookbackDays", -7);
        LocalDate expectedEndDate = LocalDate.now(IST_ZONE);

        nationalDashboardRefreshTask.runTask();

        verify(schemeRegularityService).refreshNationalDashboard(expectedEndDate, expectedEndDate);
    }
}
