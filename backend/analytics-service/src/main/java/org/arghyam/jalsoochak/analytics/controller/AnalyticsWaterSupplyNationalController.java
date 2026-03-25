package org.arghyam.jalsoochak.analytics.controller;

import org.arghyam.jalsoochak.analytics.dto.response.AverageWaterSupplyResponse;
import org.arghyam.jalsoochak.analytics.dto.response.NationalDashboardResponse;
import org.arghyam.jalsoochak.analytics.enums.WaterSupplyScope;
import org.arghyam.jalsoochak.analytics.service.DateDimensionService;
import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
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

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics - Water Supply & National Dashboard", description = "Water supply metrics, national dashboard aggregates, and date dimension utilities")
public class AnalyticsWaterSupplyNationalController {

    private final SchemeRegularityService schemeRegularityService;
    private final DateDimensionService dateDimensionService;

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
            return ResponseEntity.ok(
                    schemeRegularityService.getAverageWaterSupplyPerCurrentRegionForCurrentScope(
                            tenantId, startDate, endDate));
        }

        AverageWaterSupplyResponse response;
        if (tenantId == null) {
            if (parentLgdId != null || parentDepartmentId != null) {
                throw new IllegalArgumentException("tenant_id is required when parent_lgd_id or parent_department_id is provided");
            }
            response = schemeRegularityService.getAverageWaterSupplyPerNationForChildScope(startDate, endDate);
        } else if (parentLgdId != null) {
            response = schemeRegularityService.getAverageWaterSupplyPerCurrentRegionByLgdForChildScope(
                    tenantId, parentLgdId, startDate, endDate);
        } else if (parentDepartmentId != null) {
            response = schemeRegularityService.getAverageWaterSupplyPerCurrentRegionByDepartmentForChildScope(
                    tenantId, parentDepartmentId, startDate, endDate);
        } else {
            throw new IllegalArgumentException(
                    "Provide parent_lgd_id or parent_department_id when scope=child and tenant_id is provided");
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/national/dashboard")
    @Operation(summary = "Get national dashboard aggregates with state-wise metrics and overall outage distribution")
    public ResponseEntity<NationalDashboardResponse> getNationalDashboard(
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(schemeRegularityService.getNationalDashboardForApi(startDate, endDate));
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

