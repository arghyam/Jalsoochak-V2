package org.arghyam.jalsoochak.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.analytics.dto.response.TenantDetailsResponse;
import org.arghyam.jalsoochak.analytics.entity.DimLgdLocation;
import org.arghyam.jalsoochak.analytics.entity.DimScheme;
import org.arghyam.jalsoochak.analytics.entity.DimTenant;
import org.arghyam.jalsoochak.analytics.entity.FactMeterReading;
import org.arghyam.jalsoochak.analytics.repository.DimLgdLocationRepository;
import org.arghyam.jalsoochak.analytics.repository.DimSchemeRepository;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.FactMeterReadingRepository;
import org.arghyam.jalsoochak.analytics.service.TenantDetailsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics - Tenants & Schemes", description = "Tenant metadata, scheme dimensions, and raw meter reading queries")
public class AnalyticsTenantSchemeController {

    private final DimTenantRepository dimTenantRepository;
    private final DimLgdLocationRepository dimLgdLocationRepository;
    private final DimSchemeRepository dimSchemeRepository;
    private final FactMeterReadingRepository meterReadingRepository;
    private final TenantDetailsService tenantDetailsService;

    @GetMapping("/tenants")
    @Operation(summary = "List all tenants in the DW")
    public ResponseEntity<List<DimTenant>> getTenants() {
        return ResponseEntity.ok(dimTenantRepository.findAll());
    }

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
            DimLgdLocation tenantLevelLgd = dimLgdLocationRepository
                    .findFirstByTenantIdAndLgdLevelOrderByLgdIdAsc(tenantId, 1)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No level-1 lgd_id found for tenant_id: " + tenantId));
            parentLgdId = tenantLevelLgd.getLgdId();
        }
        if (parentDepartmentId != null) {
            return ResponseEntity.ok(
                    tenantDetailsService.getTenantDetailsByParentDepartmentWithAggregatedMetrics(
                            tenantId, parentDepartmentId, startDate, endDate));
        }
        return ResponseEntity.ok(
                tenantDetailsService.getTenantDetailsWithAggregatedMetrics(
                        tenantId, parentLgdId, startDate, endDate));
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
}

