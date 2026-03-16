package org.arghyam.jalsoochak.analytics.service;

import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.AverageWaterSupplyResponse;
import org.arghyam.jalsoochak.analytics.dto.response.NationalDashboardResponse;
import org.arghyam.jalsoochak.analytics.dto.response.OutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.PeriodicWaterQuantityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.RegionWiseWaterQuantityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;
import org.arghyam.jalsoochak.analytics.dto.response.SchemeStatusAndTopReportingResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserOutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserSubmissionStatusResponse;
import org.arghyam.jalsoochak.analytics.enums.PeriodScale;

import java.time.LocalDate;
import java.util.Map;
public interface SchemeRegularityService {

    AverageSchemeRegularityResponse getAverageSchemeRegularity(Integer parentLgdId, LocalDate startDate, LocalDate endDate);

    AverageSchemeRegularityResponse getAverageSchemeRegularityForChildRegions(
            Integer parentLgdId, LocalDate startDate, LocalDate endDate);

    ReadingSubmissionRateResponse getReadingSubmissionRateByLgd(Integer parentLgdId, LocalDate startDate, LocalDate endDate);

    ReadingSubmissionRateResponse getReadingSubmissionRateByLgdForChildRegions(
            Integer parentLgdId, LocalDate startDate, LocalDate endDate);

    AverageSchemeRegularityResponse getAverageSchemeRegularityByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate);

    AverageSchemeRegularityResponse getAverageSchemeRegularityByDepartmentForChildRegions(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate);

    ReadingSubmissionRateResponse getReadingSubmissionRateByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate);

    ReadingSubmissionRateResponse getReadingSubmissionRateByDepartmentForChildRegions(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate);

    AverageWaterSupplyResponse getAverageWaterSupplyPerCurrentRegion(
            Integer tenantId, LocalDate startDate, LocalDate endDate);

    AverageWaterSupplyResponse getAverageWaterSupplyPerNation(
            LocalDate startDate, LocalDate endDate);

    NationalDashboardResponse getNationalDashboard(
            LocalDate startDate, LocalDate endDate);

    NationalDashboardResponse refreshNationalDashboard(
            LocalDate startDate, LocalDate endDate);

    AverageWaterSupplyResponse getAverageWaterSupplyPerCurrentRegionByLgd(
            Integer tenantId, Integer lgdId, LocalDate startDate, LocalDate endDate);

    AverageWaterSupplyResponse getAverageWaterSupplyPerCurrentRegionByDepartment(
            Integer tenantId, Integer parentDepartmentId, LocalDate startDate, LocalDate endDate);

    RegionWiseWaterQuantityResponse getRegionWiseWaterQuantityByLgd(
            Integer parentLgdId, LocalDate startDate, LocalDate endDate);

    RegionWiseWaterQuantityResponse getRegionWiseWaterQuantityByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate);

    PeriodicWaterQuantityResponse getPeriodicWaterQuantityByLgdId(
            Integer lgdId, LocalDate startDate, LocalDate endDate, PeriodScale scale);

    PeriodicWaterQuantityResponse getPeriodicWaterQuantityByDepartment(
            Integer departmentId, LocalDate startDate, LocalDate endDate, PeriodScale scale);

    OutageReasonSchemeCountResponse getOutageReasonSchemeCountByLgd(
            Integer parentLgdId, LocalDate startDate, LocalDate endDate);

    OutageReasonSchemeCountResponse getOutageReasonSchemeCountByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate);

    UserOutageReasonSchemeCountResponse getOutageReasonSchemeCountByUser(
            Integer userId, LocalDate startDate, LocalDate endDate);

    UserSubmissionStatusResponse getSubmissionStatusByUser(
            Integer userId, LocalDate startDate, LocalDate endDate);

    Map<String, Integer> getSchemeStatusCountByLgd(Integer lgdId);

    Map<String, Integer> getSchemeStatusCountByDepartment(Integer departmentId);

    SchemeStatusAndTopReportingResponse getSchemeStatusAndTopReportingByLgd(
            Integer parentLgdId, LocalDate startDate, LocalDate endDate, Integer topSchemeCount);

    SchemeStatusAndTopReportingResponse getSchemeStatusAndTopReportingByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate, Integer topSchemeCount);
}
