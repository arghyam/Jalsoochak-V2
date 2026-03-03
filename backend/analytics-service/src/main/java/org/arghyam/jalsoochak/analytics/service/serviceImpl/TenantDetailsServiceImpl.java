package org.arghyam.jalsoochak.analytics.service.serviceImpl;

import org.arghyam.jalsoochak.analytics.dto.response.ChildRegionDetails;
import org.arghyam.jalsoochak.analytics.dto.response.TenantDetailsResponse;
import org.arghyam.jalsoochak.analytics.entity.DimTenant;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.TenantBoundaryRepository;
import org.arghyam.jalsoochak.analytics.service.TenantDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDetailsServiceImpl implements TenantDetailsService {

    private static final Duration TENANT_DETAILS_CACHE_TTL = Duration.ofHours(24);
    private static final String TENANT_DETAILS_CACHE_PREFIX = "analytics-service:api-cache:get_tenant_details";

    private final DimTenantRepository dimTenantRepository;
    private final TenantBoundaryRepository tenantBoundaryRepository;
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
                + ":parent:" + parentSegment;
        TenantDetailsResponse cached = readFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        DimTenant tenant = dimTenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found for tenant_id: " + tenantId));

        String schemaName = resolveTenantSchema(tenant.getStateCode());
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
                    .schemaName(schemaName)
                    .parentLgdId(null)
                    .childBoundaryCount(boundaryCount)
                    .boundaryGeoJson(boundaryGeoJson)
                    .childRegions(List.of())
                    .build();
        }

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
                .schemaName(schemaName)
                .parentLgdId(parentLgdId)
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
        if (!tenantBoundaryRepository.tableExists(schemaName, "lgd_location_master_table")) {
            throw new IllegalStateException("Missing table: " + schemaName + ".lgd_location_master_table");
        }
        if (!tenantBoundaryRepository.tableExists(schemaName, "location_config_master_table")) {
            throw new IllegalStateException("Missing table: " + schemaName + ".location_config_master_table");
        }
        if (!tenantBoundaryRepository.columnExists(schemaName, "lgd_location_master_table", "geom")) {
            throw new IllegalStateException("Missing column: " + schemaName + ".lgd_location_master_table.geom");
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
}
