package org.arghyam.jalsoochak.analytics.scheduler.task;

import org.arghyam.jalsoochak.analytics.service.DateDimensionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DimDateBackfillTaskTest {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    @Mock
    private DateDimensionService dateDimensionService;

    @InjectMocks
    private DimDateBackfillTask dimDateBackfillTask;

    @Test
    void runTask_backfillsPreviousDayInIst() {
        LocalDate expectedTargetDate = LocalDate.now(IST_ZONE).minusDays(1);

        dimDateBackfillTask.runTask();

        verify(dateDimensionService).ensureDateExists(expectedTargetDate);
    }
}
