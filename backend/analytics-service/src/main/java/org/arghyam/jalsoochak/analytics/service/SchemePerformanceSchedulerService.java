package org.arghyam.jalsoochak.analytics.service;

import java.time.LocalDate;

public interface SchemePerformanceSchedulerService {

    int insertDailySchemePerformanceScores(LocalDate targetDate);
}
