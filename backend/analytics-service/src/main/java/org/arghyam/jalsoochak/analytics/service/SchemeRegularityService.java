package org.arghyam.jalsoochak.analytics.service;

import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.AverageWaterSupplyResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;

import java.time.LocalDate;

public interface SchemeRegularityService {

    AverageSchemeRegularityResponse getAverageSchemeRegularity(Integer lgdId, LocalDate startDate, LocalDate endDate);

    ReadingSubmissionRateResponse getReadingSubmissionRate(Integer lgdId, LocalDate startDate, LocalDate endDate);

    AverageSchemeRegularityResponse getAverageSchemeRegularityByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate);

    ReadingSubmissionRateResponse getReadingSubmissionRateByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate);

    AverageWaterSupplyResponse getAverageWaterSupplyPerScheme(
            Integer tenantId, LocalDate startDate, LocalDate endDate);

    AverageWaterSupplyResponse getAverageWaterSupplyPerNation(
            LocalDate startDate, LocalDate endDate);

    AverageWaterSupplyResponse getAverageWaterSupplyPerSchemeByLgd(
            Integer tenantId, Integer lgdId, LocalDate startDate, LocalDate endDate);

    AverageWaterSupplyResponse getAverageWaterSupplyPerSchemeByDepartment(
            Integer tenantId, Integer parentDepartmentId, LocalDate startDate, LocalDate endDate);
}
