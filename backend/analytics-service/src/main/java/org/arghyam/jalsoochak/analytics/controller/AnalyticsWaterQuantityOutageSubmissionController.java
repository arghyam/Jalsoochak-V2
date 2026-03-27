package org.arghyam.jalsoochak.analytics.controller;

import org.arghyam.jalsoochak.analytics.dto.response.NonSubmissionReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.OutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.PeriodicOutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.PeriodicWaterQuantityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.RegionWiseWaterQuantityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.SubmissionStatusSummaryResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserNonSubmissionReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserOutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserSubmissionStatusResponse;
import org.arghyam.jalsoochak.analytics.enums.PeriodScale;
import org.arghyam.jalsoochak.analytics.helper.AnalyticsControllerHelper;
import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics - Water Quantity, Outages & Submission", description = "Water quantity metrics, outage/non-submission distributions, and submission status aggregates")
public class AnalyticsWaterQuantityOutageSubmissionController {

    private final SchemeRegularityService schemeRegularityService;

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
                    schemeRegularityService.getPeriodicOutageReasonSchemeCountByLgdId(lgdId, startDate, endDate, periodScale));
        }
        return ResponseEntity.ok(
                schemeRegularityService.getPeriodicOutageReasonSchemeCountByDepartment(departmentId, startDate, endDate, periodScale));
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
                schemeRegularityService.getNonSubmissionReasonSchemeCountByDepartment(parentDepartmentId, startDate, endDate));
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

    @GetMapping("/submission-status")
    @Operation(summary = "Get scheme count and compliant/anomalous submission counts for an LGD or department")
    public ResponseEntity<SubmissionStatusSummaryResponse> getSubmissionStatusSummary(
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "lgd_id", required = false) Integer lgdId,
            @RequestParam(name = "department_id", required = false) Integer departmentId) {
        if (lgdId != null && departmentId != null) {
            throw new IllegalArgumentException("Provide either lgd_id or department_id, not both");
        }
        if (lgdId == null && departmentId == null) {
            throw new IllegalArgumentException("Provide either lgd_id or department_id");
        }
        if (lgdId != null) {
            return ResponseEntity.ok(
                    schemeRegularityService.getSubmissionStatusSummaryByLgd(lgdId, startDate, endDate));
        }
        return ResponseEntity.ok(
                schemeRegularityService.getSubmissionStatusSummaryByDepartment(departmentId, startDate, endDate));
    }
}

