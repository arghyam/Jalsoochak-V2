package org.arghyam.jalsoochak.analytics.controller;

import org.arghyam.jalsoochak.analytics.entity.DimScheme;
import org.arghyam.jalsoochak.analytics.entity.DimTenant;
import org.arghyam.jalsoochak.analytics.entity.FactEscalation;
import org.arghyam.jalsoochak.analytics.entity.FactMeterReading;
import org.arghyam.jalsoochak.analytics.entity.FactSchemePerformance;
import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.AverageWaterSupplyResponse;
import org.arghyam.jalsoochak.analytics.dto.response.NonSubmissionReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.NationalDashboardResponse;
import org.arghyam.jalsoochak.analytics.dto.response.OutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.PeriodicOutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.PeriodicWaterQuantityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.PeriodicSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.RegionWiseWaterQuantityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;
import org.arghyam.jalsoochak.analytics.dto.response.SchemeRegularityListResponse;
import org.arghyam.jalsoochak.analytics.dto.response.SchemeStatusAndTopReportingResponse;
import org.arghyam.jalsoochak.analytics.dto.response.TenantDetailsResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserNonSubmissionReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserOutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserSubmissionStatusResponse;
import org.arghyam.jalsoochak.analytics.enums.PeriodScale;
import org.arghyam.jalsoochak.analytics.enums.RegularityScope;
import org.arghyam.jalsoochak.analytics.enums.WaterSupplyScope;
import org.arghyam.jalsoochak.analytics.helper.AnalyticsControllerHelper;
import org.arghyam.jalsoochak.analytics.repository.DimSchemeRepository;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.FactEscalationRepository;
import org.arghyam.jalsoochak.analytics.repository.FactMeterReadingRepository;
import org.arghyam.jalsoochak.analytics.repository.FactSchemePerformanceRepository;
import org.arghyam.jalsoochak.analytics.repository.SchemeRegularityRepository;
import org.arghyam.jalsoochak.analytics.service.DateDimensionService;
import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import org.arghyam.jalsoochak.analytics.service.TenantDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Data warehouse query endpoints")
public class AnalyticsController {
    private static final String CSV_OUTPUT_FORMAT = "csv";

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
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId,
            @RequestParam(name = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null && endDate == null) {
            endDate = LocalDate.now();
            startDate = endDate.minusDays(30);
        } else if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Provide both start_date and end_date together");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("end_date must be on or after start_date");
        }
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
            Map<Integer, BigDecimal> childPerformanceByDepartmentId = schemeRegularityService
                    .getChildAveragePerformanceScoreByDepartment(parentDepartmentId, startDate, endDate)
                    .stream()
                    .collect(Collectors.toMap(
                            SchemeRegularityRepository.ChildRegionPerformanceScore::departmentId,
                            SchemeRegularityRepository.ChildRegionPerformanceScore::averagePerformanceScore));
            if (response.getChildRegions() != null) {
                response.getChildRegions().forEach(childRegion -> childRegion.setAveragePerformanceScore(
                        childPerformanceByDepartmentId.getOrDefault(
                                childRegion.getDepartmentId(), BigDecimal.ZERO)));
            }
            response.setAveragePerformanceScore(
                    schemeRegularityService.getAveragePerformanceScoreByDepartment(
                            parentDepartmentId, startDate, endDate));
            response.setAverageSchemeRegularity(averageResponse.getAverageRegularity());
            response.setReadingSubmissionRate(submissionRateResponse.getReadingSubmissionRate());
            return ResponseEntity.ok(response);
        }
        TenantDetailsResponse response = tenantDetailsService.getTenantDetails(tenantId, parentLgdId);
        AverageSchemeRegularityResponse averageResponse = schemeRegularityService
                .getAverageSchemeRegularity(parentLgdId, startDate, endDate);
        ReadingSubmissionRateResponse submissionRateResponse = schemeRegularityService
                .getReadingSubmissionRateByLgd(parentLgdId, startDate, endDate);
        Map<Integer, BigDecimal> childPerformanceByLgdId = schemeRegularityService
                .getChildAveragePerformanceScoreByLgd(parentLgdId, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        SchemeRegularityRepository.ChildRegionPerformanceScore::lgdId,
                        SchemeRegularityRepository.ChildRegionPerformanceScore::averagePerformanceScore));
        if (response.getChildRegions() != null) {
            response.getChildRegions().forEach(childRegion -> childRegion.setAveragePerformanceScore(
                    childPerformanceByLgdId.getOrDefault(
                            childRegion.getLgdId(), BigDecimal.ZERO)));
        }
        response.setAveragePerformanceScore(
                schemeRegularityService.getAveragePerformanceScoreByLgd(
                        parentLgdId, startDate, endDate));
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

    @GetMapping("/outage-reasons/periodic")
    @Operation(summary = "Get periodic outage reason wise distinct scheme counts for an LGD ID or department (no child regions)")
    public ResponseEntity<PeriodicOutageReasonSchemeCountResponse> getPeriodicOutageReasonWiseSchemeCount(
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
                    schemeRegularityService.getPeriodicOutageReasonSchemeCountByLgdId(
                            lgdId, startDate, endDate, periodScale));
        }
        return ResponseEntity.ok(
                schemeRegularityService.getPeriodicOutageReasonSchemeCountByDepartment(
                        departmentId, startDate, endDate, periodScale));
    }

    @GetMapping("/outage-reasons/user")
    @Operation(summary = "Get outage reason wise scheme count for a user")
    @PreAuthorize("hasAnyRole('SUPER_USER', 'STATE_ADMIN')")
    public ResponseEntity<UserOutageReasonSchemeCountResponse> getOutageReasonWiseSchemeCountByUser(
            JwtAuthenticationToken authentication,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                schemeRegularityService.getOutageReasonSchemeCountByUserUuid(
                        AnalyticsControllerHelper.extractAuthenticatedUserUuid(authentication), startDate, endDate));
    }

    @GetMapping("/non-submission-reasons")
    @Operation(summary = "Get non submission reason wise scheme count for an LGD or department area")
    public ResponseEntity<NonSubmissionReasonSchemeCountResponse> getNonSubmissionReasonWiseSchemeCount(
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
                    schemeRegularityService.getNonSubmissionReasonSchemeCountByLgd(parentLgdId, startDate, endDate));
        }
        return ResponseEntity.ok(
                schemeRegularityService.getNonSubmissionReasonSchemeCountByDepartment(
                        parentDepartmentId, startDate, endDate));
    }

    @GetMapping("/non-submission-reasons/user")
    @Operation(summary = "Get non submission reason wise scheme count for a user")
    @PreAuthorize("hasAnyRole('SUPER_USER', 'STATE_ADMIN')")
    public ResponseEntity<UserNonSubmissionReasonSchemeCountResponse> getNonSubmissionReasonWiseSchemeCountByUser(
            JwtAuthenticationToken authentication,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                schemeRegularityService.getNonSubmissionReasonSchemeCountByUserUuid(
                        AnalyticsControllerHelper.extractAuthenticatedUserUuid(authentication), startDate, endDate));
    }

    @GetMapping("/submission-status/user")
    @Operation(summary = "Get submission status counts for a user")
    @PreAuthorize("hasAnyRole('SUPER_USER', 'STATE_ADMIN')")
    public ResponseEntity<UserSubmissionStatusResponse> getSubmissionStatusByUser(
            JwtAuthenticationToken authentication,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                schemeRegularityService.getSubmissionStatusByUserUuid(
                        AnalyticsControllerHelper.extractAuthenticatedUserUuid(authentication), startDate, endDate));
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

    @GetMapping("/schemes/region-report")
    @Operation(summary = "Get all schemes with status, average regularity, submission rate and submission days for a parent LGD or parent department")
    public ResponseEntity<?> getSchemeRegionReport(
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "parent_lgd_id", required = false) Integer parentLgdId,
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId,
            @RequestParam(name = "page_number", required = false) Integer pageNumber,
            @RequestParam(name = "count", required = false) Integer count,
            @Parameter(
                    description = "Output format for response",
                    required = false,
                    schema = @Schema(type = "string", allowableValues = {"json", "csv"}, defaultValue = "json", example = "csv"))
            @RequestParam(name = "output_format", required = false, defaultValue = "json") String outputFormat) {
        if (parentLgdId != null && parentDepartmentId != null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id, not both");
        }
        if (parentLgdId == null && parentDepartmentId == null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id");
        }
        SchemeRegularityListResponse reportResponse;
        if (parentLgdId != null) {
            reportResponse = schemeRegularityService.getSchemeRegionReportByLgd(
                    parentLgdId, startDate, endDate, pageNumber, count);
        } else {
            reportResponse = schemeRegularityService.getSchemeRegionReportByDepartment(
                    parentDepartmentId, startDate, endDate, pageNumber, count);
        }
        if (!CSV_OUTPUT_FORMAT.equalsIgnoreCase(Objects.toString(outputFormat, ""))) {
            return ResponseEntity.ok(reportResponse);
        }

        String csvContent = AnalyticsControllerHelper.buildSchemeRegionReportCsv(reportResponse);
        String filename = AnalyticsControllerHelper.buildSchemeRegionReportFilename(reportResponse, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvContent);
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

    @GetMapping("/scheme-regularity/periodic")
    @Operation(summary = "Get periodic average scheme regularity for an LGD ID or department")
    public ResponseEntity<PeriodicSchemeRegularityResponse> getPeriodicSchemeRegularity(
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
                    schemeRegularityService.getPeriodicSchemeRegularityByLgdId(lgdId, startDate, endDate, periodScale));
        }
        return ResponseEntity.ok(
                schemeRegularityService.getPeriodicSchemeRegularityByDepartment(departmentId, startDate, endDate, periodScale));
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
