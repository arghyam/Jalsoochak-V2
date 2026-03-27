package org.arghyam.jalsoochak.analytics.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.TenantDetailsResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;
import org.arghyam.jalsoochak.analytics.entity.DimTenant;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.SchemeRegularityRepository;
import org.arghyam.jalsoochak.analytics.repository.TenantBoundaryRepository;
import org.arghyam.jalsoochak.analytics.repository.TenantDepartmentBoundaryRepository;
import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantDetailsServiceImplTest {

    @Mock
    private DimTenantRepository dimTenantRepository;
    @Mock
    private TenantBoundaryRepository tenantBoundaryRepository;
    @Mock
    private TenantDepartmentBoundaryRepository tenantDepartmentBoundaryRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SchemeRegularityService schemeRegularityService;

    @InjectMocks
    private TenantDetailsServiceImpl service;

    @Test
    void getTenantDetails_invalidTenant_throws() {
        assertThatThrownBy(() -> service.getTenantDetails(0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant_id must be a positive integer");
    }

    @Test
    void getTenantDetails_cacheHit_returnsCachedResponse() throws Exception {
        mockRedisValueOps();
        String key = "analytics-service:api-cache:get_tenant_details:tenant:1:parent:all:v3";
        TenantDetailsResponse cached = TenantDetailsResponse.builder().tenantId(1).stateCode("mp").build();
        when(valueOperations.get(key)).thenReturn("cached");
        when(objectMapper.readValue("cached", TenantDetailsResponse.class)).thenReturn(cached);

        TenantDetailsResponse response = service.getTenantDetails(1, null);

        assertThat(response.getTenantId()).isEqualTo(1);
        verify(dimTenantRepository, never()).findById(any());
    }

    @Test
    void getTenantDetails_withoutParent_returnsTenantMergedBoundary() {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenReturn(null);
        when(dimTenantRepository.findById(1)).thenReturn(Optional.of(tenant(1, "mp")));
        when(tenantBoundaryRepository.tableExists("tenant_mp", "lgd_location_master_table")).thenReturn(true);
        when(tenantBoundaryRepository.tableExists("tenant_mp", "location_config_master_table")).thenReturn(true);
        when(tenantBoundaryRepository.columnExists("tenant_mp", "lgd_location_master_table", "geom")).thenReturn(true);
        when(tenantBoundaryRepository.getMergedBoundaryForTenant("tenant_mp"))
                .thenReturn(Map.of("boundary_count", 3, "boundary_geojson", "{\"type\":\"MultiPolygon\"}"));

        TenantDetailsResponse response = service.getTenantDetails(1, null);

        assertThat(response.getTenantId()).isEqualTo(1);
        assertThat(response.getChildBoundaryCount()).isEqualTo(3);
        assertThat(response.getChildRegions()).isEmpty();
    }

    @Test
    void getTenantDetails_withParent_returnsChildRowsAndBoundary() {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenReturn(null);
        when(dimTenantRepository.findById(1)).thenReturn(Optional.of(tenant(1, "mp")));
        when(tenantBoundaryRepository.tableExists("tenant_mp", "lgd_location_master_table")).thenReturn(true);
        when(tenantBoundaryRepository.tableExists("tenant_mp", "location_config_master_table")).thenReturn(true);
        when(tenantBoundaryRepository.columnExists("tenant_mp", "lgd_location_master_table", "geom")).thenReturn(true);
        when(tenantBoundaryRepository.getLocationLevel("tenant_mp", 100)).thenReturn(2);
        when(tenantBoundaryRepository.getChildLevelByParent("tenant_mp", 100, 1))
                .thenReturn(List.of(Map.of(
                        "lgd_id", 101,
                        "parent_lgd_id", 100,
                        "child_level", 3,
                        "scheme_count", 2,
                        "title", "Child A",
                        "lgd_code", "C101",
                        "boundary_geojson", "{\"type\":\"Polygon\"}"
                )));
        when(tenantBoundaryRepository.getMergedBoundaryByParent("tenant_mp", 100))
                .thenReturn(Map.of("child_count", 1, "boundary_geojson", "{\"type\":\"MultiPolygon\"}"));

        TenantDetailsResponse response = service.getTenantDetails(1, 100);

        assertThat(response.getChildBoundaryCount()).isEqualTo(1);
        assertThat(response.getChildRegions()).hasSize(1);
        assertThat(response.getChildRegions().getFirst().getLgdId()).isEqualTo(101);
    }

    @Test
    void getTenantDetailsByParentDepartment_invalidParent_throws() {
        assertThatThrownBy(() -> service.getTenantDetailsByParentDepartment(1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parent_department_id must be a positive integer");
    }

    @Test
    void getTenantDetailsWithAggregatedMetrics_parentLgd_mergesPerformanceIntoChildRows() {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenReturn(null);

        Integer tenantId = 1;
        Integer parentLgdId = 100;
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 3);

        when(dimTenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant(tenantId, "mp")));
        when(tenantBoundaryRepository.tableExists("tenant_mp", "lgd_location_master_table")).thenReturn(true);
        when(tenantBoundaryRepository.tableExists("tenant_mp", "location_config_master_table")).thenReturn(true);
        when(tenantBoundaryRepository.columnExists("tenant_mp", "lgd_location_master_table", "geom")).thenReturn(true);

        when(tenantBoundaryRepository.getLocationLevel("tenant_mp", parentLgdId)).thenReturn(1);
        when(tenantBoundaryRepository.getChildLevelByParent("tenant_mp", parentLgdId, tenantId))
                .thenReturn(List.of(Map.of(
                        "lgd_id", 101,
                        "parent_lgd_id", 100,
                        "child_level", 2,
                        "scheme_count", 2,
                        "title", "Child A",
                        "lgd_code", "C101",
                        "boundary_geojson", "{\"type\":\"Polygon\"}"
                )));
        when(tenantBoundaryRepository.getMergedBoundaryByParent("tenant_mp", parentLgdId))
                .thenReturn(Map.of("child_count", 1, "boundary_geojson", "{\"type\":\"MultiPolygon\"}"));

        when(schemeRegularityService.getAverageSchemeRegularity(parentLgdId, start, end))
                .thenReturn(AverageSchemeRegularityResponse.builder()
                        .averageRegularity(new BigDecimal("0.75"))
                        .build());
        when(schemeRegularityService.getReadingSubmissionRateByLgd(parentLgdId, start, end))
                .thenReturn(ReadingSubmissionRateResponse.builder()
                        .readingSubmissionRate(new BigDecimal("0.84"))
                        .build());
        when(schemeRegularityService.getChildAveragePerformanceScoreByLgd(parentLgdId, start, end))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.ChildRegionPerformanceScore(
                                101, null, new BigDecimal("0.9"))));
        when(schemeRegularityService.getAveragePerformanceScoreByLgd(parentLgdId, start, end))
                .thenReturn(new BigDecimal("0.5"));

        TenantDetailsResponse response =
                service.getTenantDetailsWithAggregatedMetrics(tenantId, parentLgdId, start, end);

        assertThat(response.getAverageSchemeRegularity()).isEqualByComparingTo("0.75");
        assertThat(response.getReadingSubmissionRate()).isEqualByComparingTo("0.84");
        assertThat(response.getAveragePerformanceScore()).isEqualByComparingTo("0.5");
        assertThat(response.getChildRegions()).hasSize(1);
        assertThat(response.getChildRegions().getFirst().getAveragePerformanceScore())
                .isEqualByComparingTo("0.9");
    }

    @Test
    void getTenantDetailsByParentDepartment_valid_returnsChildRowsAndBoundary() {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenReturn(null);
        when(dimTenantRepository.findById(1)).thenReturn(Optional.of(tenant(1, "mp")));
        when(tenantDepartmentBoundaryRepository.tableExists("analytics_schema", "dim_department_location_table"))
                .thenReturn(true);
        when(tenantDepartmentBoundaryRepository.columnExists("analytics_schema", "dim_department_location_table", "geom"))
                .thenReturn(true);
        when(tenantDepartmentBoundaryRepository.getDepartmentLevel(1, 200)).thenReturn(2);
        when(tenantDepartmentBoundaryRepository.getChildDepartmentsByParent(1, 200, 2))
                .thenReturn(List.of(Map.of(
                        "department_id", 201,
                        "parent_department_id", 200,
                        "child_level", 3,
                        "scheme_count", 5,
                        "title", "Dept Child",
                        "lgd_code", "D201",
                        "boundary_geojson", "{\"type\":\"Polygon\"}"
                )));
        when(tenantDepartmentBoundaryRepository.getMergedBoundaryByParentDepartment(1, 200, 2))
                .thenReturn(Map.of("child_count", 1, "boundary_geojson", "{\"type\":\"MultiPolygon\"}"));

        TenantDetailsResponse response = service.getTenantDetailsByParentDepartment(1, 200);

        assertThat(response.getChildBoundaryCount()).isEqualTo(1);
        assertThat(response.getChildRegions()).hasSize(1);
        assertThat(response.getChildRegions().getFirst().getDepartmentId()).isEqualTo(201);
    }

    private static DimTenant tenant(Integer id, String stateCode) {
        DimTenant tenant = new DimTenant();
        tenant.setTenantId(id);
        tenant.setStateCode(stateCode);
        return tenant;
    }

    private void mockRedisValueOps() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
}
