package org.arghyam.jalsoochak.analytics.service;

import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;

import java.time.LocalDate;

public interface SchemeRegularityService {

    AverageSchemeRegularityResponse getAverageSchemeRegularity(Integer lgdId, LocalDate startDate, LocalDate endDate);
}
