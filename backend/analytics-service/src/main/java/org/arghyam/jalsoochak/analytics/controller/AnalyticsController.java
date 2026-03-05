package org.arghyam.jalsoochak.analytics.controller;

import org.arghyam.jalsoochak.analytics.entity.DimScheme;
import org.arghyam.jalsoochak.analytics.entity.DimTenant;
import org.arghyam.jalsoochak.analytics.entity.FactEscalation;
import org.arghyam.jalsoochak.analytics.entity.FactMeterReading;
import org.arghyam.jalsoochak.analytics.entity.FactSchemePerformance;
import org.arghyam.jalsoochak.analytics.entity.FactWaterQuantity;
import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.AverageWaterSupplyResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;
import org.arghyam.jalsoochak.analytics.dto.response.TenantDetailsResponse;
import org.arghyam.jalsoochak.analytics.repository.DimSchemeRepository;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.FactEscalationRepository;
import org.arghyam.jalsoochak.analytics.repository.FactMeterReadingRepository;
import org.arghyam.jalsoochak.analytics.repository.FactSchemePerformanceRepository;
import org.arghyam.jalsoochak.analytics.repository.FactWaterQuantityRepository;
import org.arghyam.jalsoochak.analytics.service.DateDimensionService;
import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import org.arghyam.jalsoochak.analytics.service.TenantDetailsService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Data warehouse query endpoints")
public class AnalyticsController {

    private final DimTenantRepository dimTenantRepository;
    private final DimSchemeRepository dimSchemeRepository;
    private final FactMeterReadingRepository meterReadingRepository;
    private final FactWaterQuantityRepository waterQuantityRepository;
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
                .getReadingSubmissionRate(parentLgdId, startDate, endDate);
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


    @GetMapping("/water-quantity")
    @Operation(summary = "Query water quantity data by tenant or scheme and date range")
    public ResponseEntity<List<FactWaterQuantity>> getWaterQuantity(
            @RequestParam(required = false) Integer tenantId,
            @RequestParam(required = false) Integer schemeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (schemeId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(waterQuantityRepository.findBySchemeIdAndDateBetween(
                    schemeId, startDate, endDate));
        }
        if (tenantId != null && startDate != null && endDate != null) {
            return ResponseEntity.ok(waterQuantityRepository.findByTenantIdAndDateBetween(
                    tenantId, startDate, endDate));
        }
        if (schemeId != null) {
            return ResponseEntity.ok(waterQuantityRepository.findBySchemeId(schemeId));
        }
        if (tenantId != null) {
            return ResponseEntity.ok(waterQuantityRepository.findByTenantId(tenantId));
        }
        return ResponseEntity.ok(waterQuantityRepository.findAll());
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
    @Operation(summary = "Get average regularity of schemes for an LGD or department area within a date range")
    public ResponseEntity<AverageSchemeRegularityResponse> getAverageSchemeRegularity(
            @RequestParam(name = "lgd_id", required = false) Integer lgdId,
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (lgdId != null && parentDepartmentId != null) {
            throw new IllegalArgumentException("Provide either lgd_id or parent_department_id, not both");
        }
        if (parentDepartmentId != null) {
            return ResponseEntity.ok(
                    schemeRegularityService.getAverageSchemeRegularityByDepartment(parentDepartmentId, startDate, endDate));
        }
        return ResponseEntity.ok(schemeRegularityService.getAverageSchemeRegularity(lgdId, startDate, endDate));
    }

    @GetMapping("/scheme-regularity/reading-submission-rate")
    @Operation(summary = "Get reading submission rate of schemes for an LGD or department area within a date range")
    public ResponseEntity<ReadingSubmissionRateResponse> getReadingSubmissionRate(
            @RequestParam(name = "lgd_id", required = false) Integer lgdId,
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (lgdId != null && parentDepartmentId != null) {
            throw new IllegalArgumentException("Provide either lgd_id or parent_department_id, not both");
        }
        if (parentDepartmentId != null) {
            return ResponseEntity.ok(
                    schemeRegularityService.getReadingSubmissionRateByDepartment(parentDepartmentId, startDate, endDate));
        }
        return ResponseEntity.ok(schemeRegularityService.getReadingSubmissionRate(lgdId, startDate, endDate));
    }


    // This endpoint is used to get the average water supply per scheme in liters/household; 
    // tenant_id optional for nation-level state aggregates
    @GetMapping("/water-supply/average-per-scheme")
    @Operation(summary = "Get average water supply per scheme in liters/household; tenant_id optional for nation-level state aggregates")
    public ResponseEntity<AverageWaterSupplyResponse> getAverageWaterSupplyPerScheme(
            @RequestParam(name = "tenant_id", required = false) Integer tenantId,
            @RequestParam(name = "parent_lgd_id", required = false) Integer parentLgdId,
            @RequestParam(name = "parent_department_id", required = false) Integer parentDepartmentId,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (parentLgdId != null && parentDepartmentId != null) {
            throw new IllegalArgumentException("Provide either parent_lgd_id or parent_department_id, not both");
        }
        if (tenantId == null) {
            if (parentLgdId != null || parentDepartmentId != null) {
                throw new IllegalArgumentException("tenant_id is required when parent_lgd_id or parent_department_id is provided");
            }
            return ResponseEntity.ok(schemeRegularityService.getAverageWaterSupplyPerNation(startDate, endDate));
        }
        if (parentLgdId != null) {
            return ResponseEntity.ok(
                    schemeRegularityService.getAverageWaterSupplyPerSchemeByLgd(tenantId, parentLgdId, startDate, endDate));
        }
        if (parentDepartmentId != null) {
            return ResponseEntity.ok(
                    schemeRegularityService.getAverageWaterSupplyPerSchemeByDepartment(tenantId, parentDepartmentId, startDate, endDate));
        }
        return ResponseEntity.ok(schemeRegularityService.getAverageWaterSupplyPerScheme(tenantId, startDate, endDate));
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
