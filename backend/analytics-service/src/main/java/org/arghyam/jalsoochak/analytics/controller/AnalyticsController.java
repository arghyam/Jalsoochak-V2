package org.arghyam.jalsoochak.analytics.controller;

import org.arghyam.jalsoochak.analytics.entity.DimScheme;
import org.arghyam.jalsoochak.analytics.entity.DimTenant;
import org.arghyam.jalsoochak.analytics.entity.FactEscalation;
import org.arghyam.jalsoochak.analytics.entity.FactMeterReading;
import org.arghyam.jalsoochak.analytics.entity.FactSchemePerformance;
import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.AverageWaterSupplyResponse;
import org.arghyam.jalsoochak.analytics.dto.response.NationalDashboardResponse;
import org.arghyam.jalsoochak.analytics.dto.response.OutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.PeriodicWaterQuantityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.RegionWiseWaterQuantityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;
import org.arghyam.jalsoochak.analytics.dto.response.SchemeStatusAndTopReportingResponse;
import org.arghyam.jalsoochak.analytics.dto.response.TenantDetailsResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserOutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserSubmissionStatusResponse;
import org.arghyam.jalsoochak.analytics.enums.PeriodScale;
import org.arghyam.jalsoochak.analytics.enums.RegularityScope;
import org.arghyam.jalsoochak.analytics.enums.WaterSupplyScope;
import org.arghyam.jalsoochak.analytics.repository.DimSchemeRepository;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.FactEscalationRepository;
import org.arghyam.jalsoochak.analytics.repository.FactMeterReadingRepository;
import org.arghyam.jalsoochak.analytics.repository.FactSchemePerformanceRepository;
import org.arghyam.jalsoochak.analytics.service.DateDimensionService;
import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import org.arghyam.jalsoochak.analytics.service.TenantDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Data warehouse query endpoints")
public class AnalyticsController {

    private final DimTenantRepository dimTenantRepository;
    private final DimSchemeRepository dimSchemeRepository;
    private final FactMeterReadingRepository meterReadingRepository;
    private final FactEscalationRepository escalationRepository;
    private final FactSchemePerformanceRepository schemePerformanceRepository;
    private final DateDimensionService dateDimensionService;
    private final TenantDetailsService tenantDetailsService;
    private final SchemeRegularityService schemeRegularityService;

    @GetMapping("/tenants")
    @Operation(summary = "List all tenants in the DW")
    public ResponseEntity<List<DimTenant>> getTenants() {
        return ResponseEntity.ok(dimTenantRepository.findAll());
    }

    //------------------------------------
    @GetMapping("/tenant_data")
    @Operation(summary = "Get tenant boundary, filtered by parent_lgd_id or parent_department_id")
    public ResponseEntity<TenantDetailsResponse> getTenantDetails(
            @RequestParam(name = "tenant_id", required = true) Integer tenantId,
            @RequestParam(name = "parent_lgd_id", required = false) Integer parentLgdId,
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);
        if (parentLgdId != null && parentDepartmentId != null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id, not both");
        }
        if (parentLgdId == null && parentDepartmentId == null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id");
        }
        if (parentDepartmentId != null) {
            TenantDetailsResponse response = tenantDetailsService.getTenantDetailsByParentDepartment(tenantId, parentDepartmentId);
            AverageSchemeRegularityResponse averageResponse = schemeRegularityService
                    .getAverageSchemeRegularityByDepartment(parentDepartmentId, startDate, endDate);
            ReadingSubmissionRateResponse submissionRateResponse = schemeRegularityService
                    .getReadingSubmissionRateByDepartment(parentDepartmentId, startDate, endDate);
            response.setAverageSchemeRegularity(averageResponse.getAverageRegularity());
            response.setReadingSubmissionRate(submissionRateResponse.getReadingSubmissionRate());
            return ResponseEntity.ok(response);
        }
        TenantDetailsResponse response = tenantDetailsService.getTenantDetails(tenantId, parentLgdId);
        AverageSchemeRegularityResponse averageResponse = schemeRegularityService
                .getAverageSchemeRegularity(parentLgdId, startDate, endDate);
        ReadingSubmissionRateResponse submissionRateResponse = schemeRegularityService
                .getReadingSubmissionRateByLgd(parentLgdId, startDate, endDate);
        response.setAverageSchemeRegularity(averageResponse.getAverageRegularity());
        response.setReadingSubmissionRate(submissionRateResponse.getReadingSubmissionRate());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/schemes")
    @Operation(summary = "List schemes, optionally filtered by tenant")
    public ResponseEntity<List<DimScheme>> getSchemes(
            @RequestParam(required = false) Integer tenantId) {
        if (tenantId != null) {
            return ResponseEntity.ok(dimSchemeRepository.findByTenantId(tenantId));
        }
        return ResponseEntity.ok(dimSchemeRepository.findAll());
    }


    @GetMapping("/meter-readings")
    @Operation(summary = "Query meter readings by tenant or scheme and date range")
    public ResponseEntity<List<FactMeterReading>> getMeterReadings(
            @RequestParam(required = false) Integer tenantId,
            @RequestParam(required = false) Integer schemeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (schemeId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(meterReadingRepository.findBySchemeIdAndReadingDateBetween(
                    schemeId, startDate, endDate));
        }
        if (tenantId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(meterReadingRepository.findByTenantIdAndReadingDateBetween(
                    tenantId, startDate, endDate));
        }
        if (schemeId != null) {
            return ResponseEntity.ok(meterReadingRepository.findBySchemeId(schemeId));
        }
        if (tenantId != null) {
            return ResponseEntity.ok(meterReadingRepository.findByTenantId(tenantId));
        }
        return ResponseEntity.ok(meterReadingRepository.findAll());
    }


    @GetMapping("/water-quantity/region-wise")
    @Operation(summary = "Get child region-wise eWater quantity and household count by parent LGD or parent department")
    public ResponseEntity<RegionWiseWaterQuantityResponse> getWaterQuantityRegionWise(
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "parent_lgd_id", required = false) Integer parentLgdId,
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId) {
        if (parentLgdId != null && parentDepartmentId != null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id, not both");
        }
        if (parentLgdId == null && parentDepartmentId == null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id");
        }
        if (parentLgdId != null) {
            return ResponseEntity.ok(
                    schemeRegularityService.getRegionWiseWaterQuantityByLgd(parentLgdId, startDate, endDate));
        }
        return ResponseEntity.ok(
                schemeRegularityService.getRegionWiseWaterQuantityByDepartment(parentDepartmentId, startDate, endDate));
    }

    @GetMapping("/water-quantity/periodic")
    @Operation(summary = "Get periodic average water quantity and household count for an LGD ID or department")
    public ResponseEntity<PeriodicWaterQuantityResponse> getPeriodicWaterQuantity(
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(
                    description = "Time aggregation scale",
                    required = true,
                    schema = @Schema(type = "string", allowableValues = {"day", "week", "month"}))
            @RequestParam(name = "scale") String scale,
            @RequestParam(name = "lgd_id", required = false) Integer lgdId,
            @RequestParam(name = "department_id", required = false) Integer departmentId) {
        if (lgdId != null && departmentId != null) {
            throw new IllegalArgumentException("Provide either lgd_id or department_id, not both");
        }
        if (lgdId == null && departmentId == null) {
            throw new IllegalArgumentException("Provide either lgd_id or department_id");
        }
        PeriodScale periodScale = PeriodScale.fromValue(scale);
        if (lgdId != null) {
            return ResponseEntity.ok(
                    schemeRegularityService.getPeriodicWaterQuantityByLgdId(lgdId, startDate, endDate, periodScale));
        }
        return ResponseEntity.ok(
                schemeRegularityService.getPeriodicWaterQuantityByDepartment(departmentId, startDate, endDate, periodScale));
    }

    @GetMapping("/outage-reasons")
    @Operation(summary = "Get outage reason wise scheme count for an LGD or department area")
    public ResponseEntity<OutageReasonSchemeCountResponse> getOutageReasonWiseSchemeCount(
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "parent_lgd_id", required = false) Integer parentLgdId,
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId) {
        if (parentLgdId != null && parentDepartmentId != null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id, not both");
        }
        if (parentLgdId == null && parentDepartmentId == null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id");
        }
        if (parentLgdId != null) {
            return ResponseEntity.ok(
                    schemeRegularityService.getOutageReasonSchemeCountByLgd(parentLgdId, startDate, endDate));
        }
        return ResponseEntity.ok(
                schemeRegularityService.getOutageReasonSchemeCountByDepartment(parentDepartmentId, startDate, endDate));
    }

    @GetMapping("/outage-reasons/user")
    @Operation(summary = "Get outage reason wise scheme count for a user")
    public ResponseEntity<UserOutageReasonSchemeCountResponse> getOutageReasonWiseSchemeCountByUser(
            @RequestParam(name = "user_id") Integer userId,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                schemeRegularityService.getOutageReasonSchemeCountByUser(userId, startDate, endDate));
    }

    @GetMapping("/submission-status/user")
    @Operation(summary = "Get submission status counts for a user")
    public ResponseEntity<UserSubmissionStatusResponse> getSubmissionStatusByUser(
            @RequestParam(name = "user_id") Integer userId,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                schemeRegularityService.getSubmissionStatusByUser(userId, startDate, endDate));
    }

    @GetMapping("/schemes/status-count")
    @Operation(summary = "Get active and inactive scheme count for an LGD or department area")
    public ResponseEntity<Map<String, Integer>> getSchemeStatusCount(
            @RequestParam(name = "lgd_id", required = false) Integer lgdId,
            @RequestParam(name = "department_id", required = false) Integer departmentId) {
        if (lgdId != null && departmentId != null) {
            throw new IllegalArgumentException("Provide either lgd_id or department_id, not both");
        }
        if (lgdId == null && departmentId == null) {
            throw new IllegalArgumentException("Provide either lgd_id or department_id");
        }
        if (lgdId != null) {
            return ResponseEntity.ok(schemeRegularityService.getSchemeStatusCountByLgd(lgdId));
        }
        return ResponseEntity.ok(schemeRegularityService.getSchemeStatusCountByDepartment(departmentId));
    }

    @GetMapping("/schemes/dashboard")
    @Operation(summary = "Get active/inactive scheme count and top-N schemes by reporting rate for a parent LGD or parent department")
    public ResponseEntity<SchemeStatusAndTopReportingResponse> getSchemeStatusAndTopReportingRate(
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "parent_lgd_id", required = false) Integer parentLgdId,
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId,
            @RequestParam(name = "scheme_count", required = false, defaultValue = "10") Integer schemeCount) {
        if (parentLgdId != null && parentDepartmentId != null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id, not both");
        }
        if (parentLgdId == null && parentDepartmentId == null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id");
        }
        if (parentLgdId != null) {
            return ResponseEntity.ok(
                    schemeRegularityService.getSchemeStatusAndTopReportingByLgd(
                            parentLgdId, startDate, endDate, schemeCount));
        }
        return ResponseEntity.ok(
                schemeRegularityService.getSchemeStatusAndTopReportingByDepartment(
                        parentDepartmentId, startDate, endDate, schemeCount));
    }


    @GetMapping("/escalations")
    @Operation(summary = "Query escalation data by tenant or scheme")
    public ResponseEntity<List<FactEscalation>> getEscalations(
            @RequestParam(required = false) Integer tenantId,
            @RequestParam(required = false) Integer schemeId,
            @RequestParam(required = false) Integer resolutionStatus) {

        if (tenantId != null && resolutionStatus != null) {
            return ResponseEntity.ok(escalationRepository.findByTenantIdAndResolutionStatus(tenantId, resolutionStatus));
        }
        if (schemeId != null) {
            return ResponseEntity.ok(escalationRepository.findBySchemeId(schemeId));
        }
        if (tenantId != null) {
            return ResponseEntity.ok(escalationRepository.findByTenantId(tenantId));
        }
        return ResponseEntity.ok(escalationRepository.findAll());
    }


    @GetMapping("/scheme-performance")
    @Operation(summary = "Query scheme performance data")
    public ResponseEntity<List<FactSchemePerformance>> getSchemePerformance(
            @RequestParam(required = false) Integer tenantId,
            @RequestParam(required = false) Integer schemeId) {

        if (schemeId != null) {
            return ResponseEntity.ok(schemePerformanceRepository.findBySchemeId(schemeId));
        }
        if (tenantId != null) {
            return ResponseEntity.ok(schemePerformanceRepository.findByTenantId(tenantId));
        }
        return ResponseEntity.ok(schemePerformanceRepository.findAll());
    }

    @GetMapping("/scheme-regularity/average")
    @Operation(summary = "Get average scheme regularity for current area or immediate children (scope=current|child) within a date range")
    public ResponseEntity<AverageSchemeRegularityResponse> getAverageSchemeRegularity(
            @RequestParam(name = "parent_lgd_id", required = false) Integer parentLgdId,
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId,
            @Parameter(
                    description = "Response scope",
                    required = false,
                    schema = @Schema(type = "string", allowableValues = {"current", "child"}, defaultValue = "current"))
            @RequestParam(name = "scope", defaultValue = "current") String scope,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (parentLgdId != null && parentDepartmentId != null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id, not both");
        }
        RegularityScope regularityScope = RegularityScope.fromValue(scope);
        if (regularityScope == RegularityScope.CHILD) {
            if (parentDepartmentId != null) {
                return ResponseEntity.ok(
                        schemeRegularityService.getAverageSchemeRegularityByDepartmentForChildRegions(
                                parentDepartmentId, startDate, endDate));
            }
            return ResponseEntity.ok(
                    schemeRegularityService.getAverageSchemeRegularityForChildRegions(parentLgdId, startDate, endDate));
        }
        if (parentDepartmentId != null) {
            return ResponseEntity.ok(
                    schemeRegularityService.getAverageSchemeRegularityByDepartment(parentDepartmentId, startDate, endDate));
        }
        return ResponseEntity.ok(schemeRegularityService.getAverageSchemeRegularity(parentLgdId, startDate, endDate));
    }

    @GetMapping("/reading-submission-rate")
    @Operation(summary = "Get reading submission rate for current area or immediate children (scope=current|child) within a date range")
    public ResponseEntity<ReadingSubmissionRateResponse> getReadingSubmissionRateByLgd(
            @RequestParam(name = "parent_lgd_id", required = false) Integer parentLgdId,
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId,
            @Parameter(
                    description = "Response scope",
                    required = false,
                    schema = @Schema(type = "string", allowableValues = {"current", "child"}, defaultValue = "current"))
            @RequestParam(name = "scope", defaultValue = "current") String scope,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (parentLgdId != null && parentDepartmentId != null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id, not both");
        }
        RegularityScope regularityScope = RegularityScope.fromValue(scope);
        if (regularityScope == RegularityScope.CHILD) {
            if (parentDepartmentId != null) {
                return ResponseEntity.ok(
                        schemeRegularityService.getReadingSubmissionRateByDepartmentForChildRegions(
                                parentDepartmentId, startDate, endDate));
            }
            return ResponseEntity.ok(
                    schemeRegularityService.getReadingSubmissionRateByLgdForChildRegions(
                            parentLgdId, startDate, endDate));
        }
        if (parentDepartmentId != null) {
            return ResponseEntity.ok(
                    schemeRegularityService.getReadingSubmissionRateByDepartment(parentDepartmentId, startDate, endDate));
        }
        return ResponseEntity.ok(schemeRegularityService.getReadingSubmissionRateByLgd(parentLgdId, startDate, endDate));
    }


    // This endpoint is used to get the average water supply per region in liters/household; 
    // tenant_id optional for nation-level state aggregates
    @GetMapping("/water-supply/average-per-region")
    @Operation(summary = "Get average water supply per region in liters/household with response scope (current|child)")
    public ResponseEntity<AverageWaterSupplyResponse> getAverageWaterSupplyPerCurrentRegion(
            @RequestParam(name = "tenant_id", required = false) Integer tenantId,
            @RequestParam(name = "parent_lgd_id", required = false) Integer parentLgdId,
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId,
            @Parameter(
                    description = "Response scope",
                    required = false,
                    schema = @Schema(type = "string", allowableValues = {"current", "child"}, defaultValue = "current"))
            @RequestParam(name = "scope", defaultValue = "current") String scope,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (parentLgdId != null && parentDepartmentId != null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id, not both");
        }
        WaterSupplyScope waterSupplyScope = WaterSupplyScope.fromValue(scope);
        if (waterSupplyScope == WaterSupplyScope.CURRENT) {
            if (tenantId == null) {
                throw new IllegalArgumentException("tenant_id is required when scope=current");
            }
            if (parentLgdId != null || parentDepartmentId != null) {
                throw new IllegalArgumentException("parent_lgd_id or parent_department_id is not supported when scope=current");
            }
            AverageWaterSupplyResponse response =
                    schemeRegularityService.getAverageWaterSupplyPerCurrentRegion(tenantId, startDate, endDate);
            response.setChildRegionCount(null);
            response.setChildRegions(null);
            return ResponseEntity.ok(response);
        }

        AverageWaterSupplyResponse response;
        if (tenantId == null) {
            if (parentLgdId != null || parentDepartmentId != null) {
                throw new IllegalArgumentException("tenant_id is required when parent_lgd_id or parent_department_id is provided");
            }
            response = schemeRegularityService.getAverageWaterSupplyPerNation(startDate, endDate);
        } else if (parentLgdId != null) {
            response = schemeRegularityService.getAverageWaterSupplyPerCurrentRegionByLgd(tenantId, parentLgdId, startDate, endDate);
        } else if (parentDepartmentId != null) {
            response = schemeRegularityService.getAverageWaterSupplyPerCurrentRegionByDepartment(
                    tenantId, parentDepartmentId, startDate, endDate);
        } else {
            throw new IllegalArgumentException(
                    "Provide parent_lgd_id or parent_department_id when scope=child and tenant_id is provided");
        }
        response.setSchemeCount(null);
        response.setSchemes(null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/national/dashboard")
    @Operation(summary = "Get national dashboard aggregates with state-wise metrics and overall outage distribution")
    public ResponseEntity<NationalDashboardResponse> getNationalDashboard(
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(schemeRegularityService.getNationalDashboard(startDate, endDate));
    }

    @PostMapping("/date-dimension/populate")
    @Operation(summary = "Pre-populate the date dimension for a given range")
    public ResponseEntity<String> populateDateDimension(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        dateDimensionService.populateDateRange(startDate, endDate);
        return ResponseEntity.ok("Date dimension populated from " + startDate + " to " + endDate);
    }
}
