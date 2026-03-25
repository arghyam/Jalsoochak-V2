package org.arghyam.jalsoochak.analytics.controller;

import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.PeriodicSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;
import org.arghyam.jalsoochak.analytics.enums.PeriodScale;
import org.arghyam.jalsoochak.analytics.enums.RegularityScope;
import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics - Regularity & Reading Submission", description = "Scheme regularity and reading submission rate metrics")
public class AnalyticsRegularityAndReadingController {

    private final SchemeRegularityService schemeRegularityService;

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
}

