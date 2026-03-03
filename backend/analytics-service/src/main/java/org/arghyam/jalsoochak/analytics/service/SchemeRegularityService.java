package org.arghyam.jalsoochak.analytics.service;

import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;

import java.time.LocalDate;

public interface SchemeRegularityService {

    AverageSchemeRegularityResponse getAverageSchemeRegularity(Integer lgdId, LocalDate startDate, LocalDate endDate);

    ReadingSubmissionRateResponse getReadingSubmissionRate(Integer lgdId, LocalDate startDate, LocalDate endDate);
}
