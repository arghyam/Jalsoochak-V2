package com.example.analytics.service.serviceImpl;

import com.example.analytics.entity.DimDate;
import com.example.analytics.repository.DimDateRepository;
import com.example.analytics.service.DateDimensionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class DateDimensionServiceImpl implements DateDimensionService {

    private final DimDateRepository dimDateRepository;

    @Override
    @Transactional
    public void ensureDateExists(LocalDate date) {
        int dateKey = toDateKey(date);
        if (!dimDateRepository.existsById(dateKey)) {
            dimDateRepository.save(buildDimDate(date, dateKey));
            log.debug("Created dim_date entry for {}", date);
        }
    }

    @Override
    public Integer getDateKey(LocalDate date) {
        return toDateKey(date);
    }

    @Override
    @Transactional
    public void populateDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Populating dim_date from {} to {}", startDate, endDate);
        LocalDate cursor = startDate;
        int count = 0;
        while (!cursor.isAfter(endDate)) {
            int dateKey = toDateKey(cursor);
            if (!dimDateRepository.existsById(dateKey)) {
                dimDateRepository.save(buildDimDate(cursor, dateKey));
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        log.info("Populated {} new dim_date rows", count);
    }

    private int toDateKey(LocalDate date) {
        return date.getYear() * 10000 + date.getMonthValue() * 100 + date.getDayOfMonth();
    }

    private DimDate buildDimDate(LocalDate date, int dateKey) {
        DayOfWeek dow = date.getDayOfWeek();
        int month = date.getMonthValue();
        // Indian fiscal year: Apr–Mar
        int fiscalYear = month >= 4 ? date.getYear() : date.getYear() - 1;

        return DimDate.builder()
                .dateKey(dateKey)
                .fullDate(date)
                .day(date.getDayOfMonth())
                .month(month)
                .monthName(date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .quarter((month - 1) / 3 + 1)
                .year(date.getYear())
                .week(date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
                .isWeekend(dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY)
                .fiscalYear(fiscalYear)
                .build();
    }
}
