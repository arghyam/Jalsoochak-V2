package org.arghyam.jalsoochak.analytics.service.serviceImpl;

import org.arghyam.jalsoochak.analytics.dto.response.ChildRegionDetails;
import org.arghyam.jalsoochak.analytics.dto.response.TenantDetailsResponse;
import org.arghyam.jalsoochak.analytics.entity.DimTenant;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.TenantBoundaryRepository;
import org.arghyam.jalsoochak.analytics.repository.TenantDepartmentBoundaryRepository;
import org.arghyam.jalsoochak.analytics.service.TenantDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDetailsServiceImpl implements TenantDetailsService {

    private static final Duration TENANT_DETAILS_CACHE_TTL = Duration.ofHours(24);
    private static final String TENANT_DETAILS_CACHE_PREFIX = "analytics-service:api-cache:get_tenant_details";
    private static final String DEBUG_LOG_PATH = "/home/beehyv/Desktop/Codes/jalSoochak/JalSoochak_New/.cursor/debug.log";

    private final DimTenantRepository dimTenantRepository;
    private final TenantBoundaryRepository tenantBoundaryRepository;
    private final TenantDepartmentBoundaryRepository tenantDepartmentBoundaryRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public TenantDetailsResponse getTenantDetails(Integer tenantId, Integer parentLgdId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenant_id must be a positive integer");
        }

        String parentSegment = parentLgdId == null ? "all" : String.valueOf(parentLgdId);
        String cacheKey = TENANT_DETAILS_CACHE_PREFIX
                + ":tenant:" + tenantId
                + ":parent:" + parentSegment
                + ":v3";
        TenantDetailsResponse cached = readFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        DimTenant tenant = dimTenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found for tenant_id: " + tenantId));

        String schemaName = resolveTenantSchema(tenant.getStateCode());
        // #region agent log
        appendDebugLog(
                "H4",
                "TenantDetailsServiceImpl:getTenantDetails:schema_resolved",
                "Resolved tenant schema for tenant_data request",
                Map.of(
                        "tenantId", tenantId,
                        "stateCode", String.valueOf(tenant.getStateCode()),
                        "schemaName", schemaName,
                        "parentLgdId", parentLgdId == null ? "null" : parentLgdId));
        // #endregion
        assertRequiredTables(schemaName);

        TenantDetailsResponse response;
        if (parentLgdId != null) {
            response = getTenantDetailsByParent(tenant, schemaName, parentLgdId);
        } else {
            Map<String, Object> boundaryResult = tenantBoundaryRepository.getMergedBoundaryForTenant(schemaName);
            Integer boundaryCount = (Integer) boundaryResult.get("boundary_count");
            String boundaryGeoJson = (String) boundaryResult.get("boundary_geojson");

            response = TenantDetailsResponse.builder()
                    .tenantId(tenant.getTenantId())
                    .stateCode(tenant.getStateCode())
                    .childBoundaryCount(boundaryCount)
                    .boundaryGeoJson(boundaryGeoJson)
                    .childRegions(List.of())
                    .build();
        }

        writeToCache(cacheKey, response);
        return response;
    }

    @Override
    public TenantDetailsResponse getTenantDetailsByParentDepartment(Integer tenantId, Integer parentDepartmentId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenant_id must be a positive integer");
        }
        if (parentDepartmentId == null || parentDepartmentId <= 0) {
            throw new IllegalArgumentException("parent_department_id must be a positive integer");
        }

        String cacheKey = TENANT_DETAILS_CACHE_PREFIX
                + ":tenant:" + tenantId
                + ":parent_department:" + parentDepartmentId
                + ":v2";
        TenantDetailsResponse cached = readFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        DimTenant tenant = dimTenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found for tenant_id: " + tenantId));

        String schemaName = resolveTenantSchema(tenant.getStateCode());
        assertRequiredDepartmentTables();

        Integer parentLevel = tenantDepartmentBoundaryRepository.getDepartmentLevel(tenantId, parentDepartmentId);
        if (parentLevel == null) {
            throw new IllegalArgumentException("parent_department_id not found for tenant: " + parentDepartmentId);
        }
        if (parentLevel >= 6) {
            throw new IllegalArgumentException("No child department level available for parent_department_id: " + parentDepartmentId);
        }

        List<Map<String, Object>> childRows = tenantDepartmentBoundaryRepository
                .getChildDepartmentsByParent(tenantId, parentDepartmentId, parentLevel);
        List<ChildRegionDetails> childRegions = childRows.stream()
                .map(row -> ChildRegionDetails.builder()
                        .departmentId((Integer) row.get("department_id"))
                        .parentLgdId(null)
                        .parentDepartmentId((Integer) row.get("parent_department_id"))
                        .lgdLevel((Integer) row.get("child_level"))
                        .schemeCount(row.get("scheme_count") instanceof Number number ? number.intValue() : 0)
                        .title((String) row.get("title"))
                        .lgdCode((String) row.get("lgd_code"))
                        .boundaryGeoJson((String) row.get("boundary_geojson"))
                        .build())
                .toList();

        Map<String, Object> mergedBoundaryResult = tenantDepartmentBoundaryRepository
                .getMergedBoundaryByParentDepartment(tenantId, parentDepartmentId, parentLevel);

        TenantDetailsResponse response = TenantDetailsResponse.builder()
                .tenantId(tenant.getTenantId())
                .stateCode(tenant.getStateCode())
                .childBoundaryCount((Integer) mergedBoundaryResult.get("child_count"))
                .boundaryGeoJson((String) mergedBoundaryResult.get("boundary_geojson"))
                .childRegions(childRegions)
                .build();

        writeToCache(cacheKey, response);
        return response;
    }

    private TenantDetailsResponse getTenantDetailsByParent(
            DimTenant tenant,
            String schemaName,
            Integer parentLgdId
    ) {
        if (parentLgdId <= 0) {
            throw new IllegalArgumentException("parent_lgd_id must be a positive integer");
        }

        Integer parentLevel = tenantBoundaryRepository.getLocationLevel(schemaName, parentLgdId);
        if (parentLevel == null) {
            throw new IllegalArgumentException("parent_lgd_id not found in schema: " + parentLgdId);
        }

        List<Map<String, Object>> childRows =
                tenantBoundaryRepository.getChildLevelByParent(schemaName, parentLgdId, tenant.getTenantId());
        List<ChildRegionDetails> childRegions = childRows.stream()
                .map(row -> ChildRegionDetails.builder()
                        .lgdId((Integer) row.get("lgd_id"))
                        .parentLgdId((Integer) row.get("parent_lgd_id"))
                        .parentDepartmentId(null)
                        .lgdLevel((Integer) row.get("child_level"))
                        .schemeCount(row.get("scheme_count") instanceof Number number ? number.intValue() : 0)
                        .title((String) row.get("title"))
                        .lgdCode((String) row.get("lgd_code"))
                        .boundaryGeoJson((String) row.get("boundary_geojson"))
                        .build())
                .toList();

        Map<String, Object> mergedBoundaryResult =
                tenantBoundaryRepository.getMergedBoundaryByParent(schemaName, parentLgdId);

        return TenantDetailsResponse.builder()
                .tenantId(tenant.getTenantId())
                .stateCode(tenant.getStateCode())
                .childBoundaryCount((Integer) mergedBoundaryResult.get("child_count"))
                .boundaryGeoJson((String) mergedBoundaryResult.get("boundary_geojson"))
                .childRegions(childRegions)
                .build();
    }

    private String resolveTenantSchema(String stateCode) {
        if (stateCode == null || stateCode.isBlank()) {
            throw new IllegalStateException("State code is missing for this tenant");
        }
        String normalized = stateCode.trim().toLowerCase();
        if (!normalized.matches("^[a-z0-9_]+$")) {
            throw new IllegalStateException("Invalid tenant state code: " + stateCode);
        }
        return "tenant_" + normalized;
    }

    private void assertRequiredTables(String schemaName) {
        boolean lgdTableExists = tenantBoundaryRepository.tableExists(schemaName, "lgd_location_master_table");
        boolean configTableExists = tenantBoundaryRepository.tableExists(schemaName, "location_config_master_table");
        boolean geomColumnExists = tenantBoundaryRepository.columnExists(schemaName, "lgd_location_master_table", "geom");
        // #region agent log
        appendDebugLog(
                "H5",
                "TenantDetailsServiceImpl:assertRequiredTables:existence_check",
                "Tenant schema dependency check",
                Map.of(
                        "schemaName", schemaName,
                        "lgdTableExists", lgdTableExists,
                        "configTableExists", configTableExists,
                        "geomColumnExists", geomColumnExists));
        // #endregion
        if (!lgdTableExists) {
            throw new IllegalStateException("Missing table: " + schemaName + ".lgd_location_master_table");
        }
        if (!configTableExists) {
            throw new IllegalStateException("Missing table: " + schemaName + ".location_config_master_table");
        }
        if (!geomColumnExists) {
            throw new IllegalStateException("Missing column: " + schemaName + ".lgd_location_master_table.geom");
        }
    }

    private void assertRequiredDepartmentTables() {
        if (!tenantDepartmentBoundaryRepository.tableExists("analytics_schema", "dim_department_location_table")) {
            throw new IllegalStateException("Missing table: analytics_schema.dim_department_location_table");
        }
        if (!tenantDepartmentBoundaryRepository.columnExists("analytics_schema", "dim_department_location_table", "geom")) {
            throw new IllegalStateException("Missing column: analytics_schema.dim_department_location_table.geom");
        }
    }

    private TenantDetailsResponse readFromCache(String cacheKey) {
        try {
            String payload = redisTemplate.opsForValue().get(cacheKey);
            if (payload == null || payload.isBlank()) {
                return null;
            }
            return objectMapper.readValue(payload, TenantDetailsResponse.class);
        } catch (Exception e) {
            log.warn("Failed to read tenant details cache [{}]: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    private void writeToCache(String cacheKey, TenantDetailsResponse response) {
        try {
            String payload = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, payload, TENANT_DETAILS_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to write tenant details cache [{}]: {}", cacheKey, e.getMessage());
        }
    }

    private void appendDebugLog(String hypothesisId, String location, String message, Map<String, Object> data) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("runId", "pre-fix");
            payload.put("hypothesisId", hypothesisId);
            payload.put("location", location);
            payload.put("message", message);
            payload.put("data", data);
            payload.put("timestamp", System.currentTimeMillis());
            Files.writeString(
                    Path.of(DEBUG_LOG_PATH),
                    objectMapper.writeValueAsString(payload) + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Swallow debug logging failures.
        }
    }
}
