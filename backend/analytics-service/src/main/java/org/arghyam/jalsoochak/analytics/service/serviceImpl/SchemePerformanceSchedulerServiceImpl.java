package org.arghyam.jalsoochak.analytics.service.serviceImpl;

import org.arghyam.jalsoochak.analytics.repository.SchemePerformanceSchedulerRepository;
import org.arghyam.jalsoochak.analytics.service.SchemePerformanceSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SchemePerformanceSchedulerServiceImpl implements SchemePerformanceSchedulerService {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private final SchemePerformanceSchedulerRepository schemePerformanceSchedulerRepository;

    @Override
    @Transactional
    public int insertDailySchemePerformanceScores(LocalDate targetDate) {
        LocalDate effectiveDate = targetDate != null ? targetDate : LocalDate.now(IST_ZONE);
        return schemePerformanceSchedulerRepository.insertDailySchemePerformanceScores(effectiveDate);
    }
}
