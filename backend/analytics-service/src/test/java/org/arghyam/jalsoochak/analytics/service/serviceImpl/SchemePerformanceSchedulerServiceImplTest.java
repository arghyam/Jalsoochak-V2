package org.arghyam.jalsoochak.analytics.service.serviceImpl;

import org.arghyam.jalsoochak.analytics.repository.SchemePerformanceSchedulerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemePerformanceSchedulerServiceImplTest {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    @Mock
    private SchemePerformanceSchedulerRepository schemePerformanceSchedulerRepository;

    @InjectMocks
    private SchemePerformanceSchedulerServiceImpl schemePerformanceSchedulerService;

    @Test
    void insertDailySchemePerformanceScores_withProvidedDate_usesSameDate() {
        LocalDate targetDate = LocalDate.of(2026, 3, 17);
        when(schemePerformanceSchedulerRepository.insertDailySchemePerformanceScores(targetDate)).thenReturn(6);

        int insertedRows = schemePerformanceSchedulerService.insertDailySchemePerformanceScores(targetDate);

        assertThat(insertedRows).isEqualTo(6);
        verify(schemePerformanceSchedulerRepository).insertDailySchemePerformanceScores(targetDate);
    }

    @Test
    void insertDailySchemePerformanceScores_withNullDate_defaultsToIstToday() {
        LocalDate todayIst = LocalDate.now(IST_ZONE);
        when(schemePerformanceSchedulerRepository.insertDailySchemePerformanceScores(todayIst)).thenReturn(4);

        int insertedRows = schemePerformanceSchedulerService.insertDailySchemePerformanceScores(null);

        assertThat(insertedRows).isEqualTo(4);
        verify(schemePerformanceSchedulerRepository).insertDailySchemePerformanceScores(todayIst);
    }
}
