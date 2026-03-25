package org.arghyam.jalsoochak.analytics.controller;

import org.arghyam.jalsoochak.analytics.dto.response.SchemeRegularityListResponse;
import org.arghyam.jalsoochak.analytics.dto.response.SchemeStatusAndTopReportingResponse;
import org.arghyam.jalsoochak.analytics.entity.FactEscalation;
import org.arghyam.jalsoochak.analytics.entity.FactSchemePerformance;
import org.arghyam.jalsoochak.analytics.helper.AnalyticsControllerHelper;
import org.arghyam.jalsoochak.analytics.repository.FactEscalationRepository;
import org.arghyam.jalsoochak.analytics.repository.FactSchemePerformanceRepository;
import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics - Scheme Reporting", description = "Scheme dashboards, region reports (CSV/JSON), escalations, and scheme performance queries")
public class AnalyticsSchemeReportingController {

    private static final String CSV_OUTPUT_FORMAT = "csv";

    private final FactEscalationRepository escalationRepository;
    private final FactSchemePerformanceRepository schemePerformanceRepository;
    private final SchemeRegularityService schemeRegularityService;

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
}

