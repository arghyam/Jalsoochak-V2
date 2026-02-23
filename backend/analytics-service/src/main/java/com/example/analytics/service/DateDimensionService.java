package com.example.analytics.service;

import java.time.LocalDate;

public interface DateDimensionService {

    void ensureDateExists(LocalDate date);

    Integer getDateKey(LocalDate date);

    void populateDateRange(LocalDate startDate, LocalDate endDate);
}
