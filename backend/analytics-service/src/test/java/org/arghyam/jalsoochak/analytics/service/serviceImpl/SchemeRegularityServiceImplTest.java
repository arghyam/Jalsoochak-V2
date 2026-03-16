package org.arghyam.jalsoochak.analytics.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.AverageWaterSupplyResponse;
import org.arghyam.jalsoochak.analytics.dto.response.NationalDashboardResponse;
import org.arghyam.jalsoochak.analytics.dto.response.OutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.PeriodicWaterQuantityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserOutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserSubmissionStatusResponse;
import org.arghyam.jalsoochak.analytics.entity.DimTenant;
import org.arghyam.jalsoochak.analytics.enums.PeriodScale;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.SchemeRegularityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemeRegularityServiceImplTest {

    @Mock
    private SchemeRegularityRepository schemeRegularityRepository;
    @Mock
    private DimTenantRepository dimTenantRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SchemeRegularityServiceImpl service;

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 1, 3);

    @Test
    void getAverageSchemeRegularity_invalidLgd_throwsBadRequest() {
        assertThatThrownBy(() -> service.getAverageSchemeRegularity(0, START, END))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lgd_id must be a positive integer");
    }

    @Test
    void getAverageSchemeRegularity_invalidDateRange_throwsBadRequest() {
        assertThatThrownBy(() -> service.getAverageSchemeRegularity(101, END, START))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("end_date must be on or after start_date");
    }

    @Test
    void getAverageSchemeRegularity_cacheHit_returnsCachedAndSkipsRepository() throws Exception {
        mockRedisValueOps();
        String key = ":scheme_regularity:lgd:101:start:2026-01-01:end:2026-01-03";
        AverageSchemeRegularityResponse cached = AverageSchemeRegularityResponse.builder()
                .lgdId(101)
                .averageRegularity(new BigDecimal("0.7777"))
                .build();
        when(valueOperations.get(key)).thenReturn("cached");
        when(objectMapper.readValue("cached", AverageSchemeRegularityResponse.class)).thenReturn(cached);

        AverageSchemeRegularityResponse response = service.getAverageSchemeRegularity(101, START, END);

        assertThat(response.getAverageRegularity()).isEqualByComparingTo("0.7777");
        verify(schemeRegularityRepository, never()).getSchemeRegularityMetrics(any(), any(), any());
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void getAverageSchemeRegularity_cacheMiss_computesAndWritesCache() throws Exception {
        mockRedisValueOps();
        String key = ":scheme_regularity:lgd:101:start:2026-01-01:end:2026-01-03";
        when(valueOperations.get(key)).thenReturn(null);
        when(schemeRegularityRepository.getSchemeRegularityMetrics(101, START, END))
                .thenReturn(new SchemeRegularityRepository.SchemeRegularityMetrics(2, 3));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        AverageSchemeRegularityResponse response = service.getAverageSchemeRegularity(101, START, END);

        assertThat(response.getDaysInRange()).isEqualTo(3);
        assertThat(response.getSchemeCount()).isEqualTo(2);
        assertThat(response.getTotalSupplyDays()).isEqualTo(3);
        assertThat(response.getAverageRegularity()).isEqualByComparingTo("0.5000");
        verify(valueOperations, times(1)).set(eq(key), eq("{json}"), eq(Duration.ofHours(24)));
    }

    @Test
    void getAverageSchemeRegularity_cacheReadFailure_fallsBackToRepository() throws Exception {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenThrow(new RuntimeException("redis read failed"));
        when(schemeRegularityRepository.getSchemeRegularityMetrics(101, START, END))
                .thenReturn(new SchemeRegularityRepository.SchemeRegularityMetrics(1, 1));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        AverageSchemeRegularityResponse response = service.getAverageSchemeRegularity(101, START, END);

        assertThat(response.getAverageRegularity()).isEqualByComparingTo("0.3333");
        verify(schemeRegularityRepository, times(1)).getSchemeRegularityMetrics(101, START, END);
    }

    @Test
    void getAverageSchemeRegularityForChildRegions_whenLevelHasNoChildren_throws() {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenReturn(null);
        when(schemeRegularityRepository.getLgdLevel(101)).thenReturn(6);

        assertThatThrownBy(() -> service.getAverageSchemeRegularityForChildRegions(101, START, END))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No child LGD level available");
    }

    @Test
    void getReadingSubmissionRateByDepartmentForChildRegions_aggregatesChildrenCorrectly() throws Exception {
        mockRedisValueOps();
        String key = ":reading_submission_rate:department:201:scope:child:start:2026-01-01:end:2026-01-03:v3";
        when(valueOperations.get(key)).thenReturn(null);
        when(schemeRegularityRepository.getDepartmentLevel(201)).thenReturn(2);
        when(schemeRegularityRepository.getChildReadingSubmissionRateMetricsByDepartment(201, START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.ChildRegionReadingSubmissionMetrics(
                                null, 301, "Block A", 2, 6, new BigDecimal("1.0000")),
                        new SchemeRegularityRepository.ChildRegionReadingSubmissionMetrics(
                                null, 302, "Block B", 1, 2, new BigDecimal("0.6667"))
                ));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        ReadingSubmissionRateResponse response =
                service.getReadingSubmissionRateByDepartmentForChildRegions(201, START, END);

        assertThat(response.getDaysInRange()).isEqualTo(3);
        assertThat(response.getSchemeCount()).isEqualTo(3);
        assertThat(response.getTotalSubmissionDays()).isEqualTo(8);
        assertThat(response.getReadingSubmissionRate()).isEqualByComparingTo("0.8889");
        assertThat(response.getChildRegionCount()).isEqualTo(2);
        assertThat(response.getChildRegions()).hasSize(2);
        verify(valueOperations, times(1)).set(eq(key), eq("{json}"), eq(Duration.ofHours(24)));
    }

    @Test
    void getPeriodicWaterQuantityByLgdId_capsPeriodEndDateAtRequestedEndDate() {
        LocalDate requestedEnd = LocalDate.of(2026, 1, 10);
        when(schemeRegularityRepository.getPeriodicWaterQuantityByLgdId(101, START, requestedEnd, PeriodScale.WEEK))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.PeriodicWaterQuantityMetrics(
                                LocalDate.of(2026, 1, 6),
                                LocalDate.of(2026, 1, 12),
                                "2026-W02",
                                new BigDecimal("50.1250"),
                                77)
                ));

        PeriodicWaterQuantityResponse response =
                service.getPeriodicWaterQuantityByLgdId(101, START, requestedEnd, PeriodScale.WEEK);

        assertThat(response.getScale()).isEqualTo("week");
        assertThat(response.getPeriodCount()).isEqualTo(1);
        assertThat(response.getMetrics().getFirst().getPeriodEndDate()).isEqualTo(requestedEnd);
        assertThat(response.getMetrics().getFirst().getAverageWaterQuantity()).isEqualByComparingTo("50.1250");
    }

    @Test
    void getOutageReasonSchemeCountByLgd_fillsMissingReasonKeysAndChildRows() {
        when(schemeRegularityRepository.getLgdLevel(101)).thenReturn(3);
        when(schemeRegularityRepository.getOutageReasonSchemeCountByLgd(101, START, END))
                .thenReturn(List.of(new SchemeRegularityRepository.OutageReasonSchemeCount(2, 4)));
        when(schemeRegularityRepository.getChildRegionsByLgd(101))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.ChildRegionRef(401, null, "Village A"),
                        new SchemeRegularityRepository.ChildRegionRef(402, null, "Village B")
                ));
        when(schemeRegularityRepository.getChildOutageReasonSchemeCountByLgd(101, START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.ChildRegionOutageReasonSchemeCount(401, null, 1, 2),
                        new SchemeRegularityRepository.ChildRegionOutageReasonSchemeCount(999, null, 3, 5)
                ));

        OutageReasonSchemeCountResponse response =
                service.getOutageReasonSchemeCountByLgd(101, START, END);

        assertThat(response.getOutageReasonSchemeCount())
                .containsEntry("draught", 0)
                .containsEntry("no_electricity", 4)
                .containsEntry("motor_burnt", 0);
        assertThat(response.getChildRegions()).hasSize(2);
        assertThat(response.getChildRegions().get(0).getOutageReasonSchemeCount())
                .containsEntry("draught", 2)
                .containsEntry("no_electricity", 0)
                .containsEntry("motor_burnt", 0);
        assertThat(response.getChildRegions().get(1).getOutageReasonSchemeCount())
                .containsEntry("draught", 0)
                .containsEntry("no_electricity", 0)
                .containsEntry("motor_burnt", 0);
    }

    @Test
    void getSchemeStatusCountByLgd_handlesNullCountsAsZero() {
        when(schemeRegularityRepository.getSchemeStatusCountByLgd(101))
                .thenReturn(new SchemeRegularityRepository.SchemeStatusCount(null, 7));

        Map<String, Integer> result = service.getSchemeStatusCountByLgd(101);

        assertThat(result)
                .containsEntry("active_schemes_count", 0)
                .containsEntry("inactive_schemes_count", 7);
    }

    @Test
    void getAverageWaterSupplyPerCurrentRegion_whenTenantMissing_throws() {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenReturn(null);
        when(schemeRegularityRepository.getAverageWaterSupplyPerCurrentRegion(10, START, END))
                .thenReturn(List.of(new SchemeRegularityRepository.SchemeWaterSupplyMetrics(
                        1, "Scheme X", 100, 1000L, 2, new BigDecimal("5.0000")
                )));
        when(dimTenantRepository.findById(10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAverageWaterSupplyPerCurrentRegion(10, START, END))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant not found for tenant_id: 10");
    }

    @Test
    void getAverageWaterSupplyPerNation_cacheHit_skipsRepository() throws Exception {
        mockRedisValueOps();
        String key = ":water_supply:nation:start:2026-01-01:end:2026-01-03:v3";
        AverageWaterSupplyResponse cached = AverageWaterSupplyResponse.builder()
                .childRegionCount(1)
                .build();
        when(valueOperations.get(key)).thenReturn("cached");
        when(objectMapper.readValue("cached", AverageWaterSupplyResponse.class)).thenReturn(cached);

        AverageWaterSupplyResponse response = service.getAverageWaterSupplyPerNation(START, END);

        assertThat(response.getChildRegionCount()).isEqualTo(1);
        verify(schemeRegularityRepository, never()).getAverageWaterSupplyPerNation(any(), any());
    }

    @Test
    void getPeriodicWaterQuantityByDepartment_withNullScale_throws() {
        assertThatThrownBy(() -> service.getPeriodicWaterQuantityByDepartment(201, START, END, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scale is required");
    }

    @Test
    void getAverageWaterSupplyPerCurrentRegionByLgd_whenLgdMissing_throws() {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenReturn(null);
        when(schemeRegularityRepository.getLgdLevel(101)).thenReturn(null);

        assertThatThrownBy(() -> service.getAverageWaterSupplyPerCurrentRegionByLgd(10, 101, START, END))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lgd_id not found");
    }

    @Test
    void getAverageWaterSupplyPerCurrentRegionByDepartment_whenDepartmentMissing_throws() {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenReturn(null);
        when(schemeRegularityRepository.getDepartmentLevel(201)).thenReturn(null);

        assertThatThrownBy(() -> service.getAverageWaterSupplyPerCurrentRegionByDepartment(10, 201, START, END))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parent_department_id not found");
    }

    @Test
    void getAverageWaterSupplyPerCurrentRegionByLgd_valid_buildsChildResponse() throws Exception {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenReturn(null);
        when(schemeRegularityRepository.getLgdLevel(101)).thenReturn(3);
        when(schemeRegularityRepository.getAverageWaterSupplyPerCurrentRegionByLgd(10, 101, START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.ChildRegionWaterSupplyMetrics(
                                null, null, 401, null, "Village A", 100, 10000L, 2, new BigDecimal("50.0000"))
                ));
        when(dimTenantRepository.findById(10)).thenReturn(Optional.of(tenant(10, "mp")));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        AverageWaterSupplyResponse response =
                service.getAverageWaterSupplyPerCurrentRegionByLgd(10, 101, START, END);

        assertThat(response.getTenantId()).isEqualTo(10);
        assertThat(response.getStateCode()).isEqualTo("mp");
        assertThat(response.getParentLgdLevel()).isEqualTo(3);
        assertThat(response.getChildRegionCount()).isEqualTo(1);
        assertThat(response.getChildRegions().getFirst().getLgdId()).isEqualTo(401);
    }

    @Test
    void getReadingSubmissionRateByLgd_cacheMiss_computesAndWritesCache() throws Exception {
        mockRedisValueOps();
        String key = ":reading_submission_rate:lgd:101:start:2026-01-01:end:2026-01-03:v3";
        when(valueOperations.get(key)).thenReturn(null);
        when(schemeRegularityRepository.getLgdLevel(101)).thenReturn(2);
        when(schemeRegularityRepository.getReadingSubmissionRateMetricsByLgd(101, START, END))
                .thenReturn(new SchemeRegularityRepository.SchemeRegularityMetrics(2, 3));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        ReadingSubmissionRateResponse response = service.getReadingSubmissionRateByLgd(101, START, END);

        assertThat(response.getParentLgdLevel()).isEqualTo(2);
        assertThat(response.getReadingSubmissionRate()).isEqualByComparingTo("0.5000");
        verify(valueOperations, times(1)).set(eq(key), eq("{json}"), eq(Duration.ofHours(24)));
    }

    @Test
    void getAverageSchemeRegularityByDepartment_cacheMiss_returnsComputedResponse() throws Exception {
        mockRedisValueOps();
        String key = ":scheme_regularity:department:201:start:2026-01-01:end:2026-01-03";
        when(valueOperations.get(key)).thenReturn(null);
        when(schemeRegularityRepository.getSchemeRegularityMetricsByDepartment(201, START, END))
                .thenReturn(new SchemeRegularityRepository.SchemeRegularityMetrics(2, 4));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        AverageSchemeRegularityResponse response =
                service.getAverageSchemeRegularityByDepartment(201, START, END);

        assertThat(response.getParentDepartmentId()).isEqualTo(201);
        assertThat(response.getAverageRegularity()).isEqualByComparingTo("0.6667");
    }

    @Test
    void getAverageSchemeRegularityByDepartmentForChildRegions_aggregatesChildRows() throws Exception {
        mockRedisValueOps();
        String key = ":scheme_regularity:department:201:scope:child:start:2026-01-01:end:2026-01-03";
        when(valueOperations.get(key)).thenReturn(null);
        when(schemeRegularityRepository.getDepartmentLevel(201)).thenReturn(2);
        when(schemeRegularityRepository.getChildSchemeRegularityMetricsByDepartment(201, START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.ChildRegionSchemeRegularityMetrics(
                                null, 301, "Dept-A", 2, 4, new BigDecimal("0.6667")),
                        new SchemeRegularityRepository.ChildRegionSchemeRegularityMetrics(
                                null, 302, "Dept-B", 1, 1, new BigDecimal("0.3333"))
                ));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        AverageSchemeRegularityResponse response =
                service.getAverageSchemeRegularityByDepartmentForChildRegions(201, START, END);

        assertThat(response.getChildRegionCount()).isEqualTo(2);
        assertThat(response.getSchemeCount()).isEqualTo(3);
        assertThat(response.getTotalSupplyDays()).isEqualTo(5);
        assertThat(response.getAverageRegularity()).isEqualByComparingTo("0.5556");
    }

    @Test
    void getReadingSubmissionRateByDepartment_cacheMiss_returnsComputedResponse() throws Exception {
        mockRedisValueOps();
        String key = ":reading_submission_rate:department:201:start:2026-01-01:end:2026-01-03:v3";
        when(valueOperations.get(key)).thenReturn(null);
        when(schemeRegularityRepository.getDepartmentLevel(201)).thenReturn(2);
        when(schemeRegularityRepository.getReadingSubmissionRateMetricsByDepartment(201, START, END))
                .thenReturn(new SchemeRegularityRepository.SchemeRegularityMetrics(2, 5));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        ReadingSubmissionRateResponse response =
                service.getReadingSubmissionRateByDepartment(201, START, END);

        assertThat(response.getParentDepartmentLevel()).isEqualTo(2);
        assertThat(response.getReadingSubmissionRate()).isEqualByComparingTo("0.8333");
    }

    @Test
    void getReadingSubmissionRateByLgdForChildRegions_aggregatesChildRows() throws Exception {
        mockRedisValueOps();
        String key = ":reading_submission_rate:lgd:101:scope:child:start:2026-01-01:end:2026-01-03:v3";
        when(valueOperations.get(key)).thenReturn(null);
        when(schemeRegularityRepository.getLgdLevel(101)).thenReturn(2);
        when(schemeRegularityRepository.getChildReadingSubmissionRateMetricsByLgd(101, START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.ChildRegionReadingSubmissionMetrics(
                                401, null, "LGD-A", 1, 3, new BigDecimal("1.0000")),
                        new SchemeRegularityRepository.ChildRegionReadingSubmissionMetrics(
                                402, null, "LGD-B", 2, 4, new BigDecimal("0.6667"))
                ));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        ReadingSubmissionRateResponse response =
                service.getReadingSubmissionRateByLgdForChildRegions(101, START, END);

        assertThat(response.getChildRegionCount()).isEqualTo(2);
        assertThat(response.getSchemeCount()).isEqualTo(3);
        assertThat(response.getTotalSubmissionDays()).isEqualTo(7);
        assertThat(response.getReadingSubmissionRate()).isEqualByComparingTo("0.7778");
    }

    @Test
    void getAverageWaterSupplyPerCurrentRegion_valid_returnsSchemeMetrics() throws Exception {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenReturn(null);
        when(schemeRegularityRepository.getAverageWaterSupplyPerCurrentRegion(10, START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.SchemeWaterSupplyMetrics(
                                1, "Scheme-A", 100, 1200L, 2, new BigDecimal("4.0000"))
                ));
        when(dimTenantRepository.findById(10)).thenReturn(Optional.of(tenant(10, "mp")));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        AverageWaterSupplyResponse response =
                service.getAverageWaterSupplyPerCurrentRegion(10, START, END);

        assertThat(response.getTenantId()).isEqualTo(10);
        assertThat(response.getStateCode()).isEqualTo("mp");
        assertThat(response.getSchemeCount()).isEqualTo(1);
        assertThat(response.getSchemes()).hasSize(1);
    }

    @Test
    void getAverageWaterSupplyPerCurrentRegionByDepartment_valid_buildsChildResponse() throws Exception {
        mockRedisValueOps();
        when(valueOperations.get(any())).thenReturn(null);
        when(schemeRegularityRepository.getDepartmentLevel(201)).thenReturn(2);
        when(schemeRegularityRepository.getAverageWaterSupplyPerCurrentRegionByDepartment(10, 201, START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.ChildRegionWaterSupplyMetrics(
                                null, null, null, 501, "Dept-1", 120, 9000L, 3, new BigDecimal("3000.0000"))
                ));
        when(dimTenantRepository.findById(10)).thenReturn(Optional.of(tenant(10, "mp")));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        AverageWaterSupplyResponse response =
                service.getAverageWaterSupplyPerCurrentRegionByDepartment(10, 201, START, END);

        assertThat(response.getParentDepartmentLevel()).isEqualTo(2);
        assertThat(response.getChildRegionCount()).isEqualTo(1);
        assertThat(response.getChildRegions().getFirst().getDepartmentId()).isEqualTo(501);
    }

    @Test
    void getRegionWiseWaterQuantityByLgd_valid_mapsChildMetrics() {
        when(schemeRegularityRepository.getLgdLevel(101)).thenReturn(2);
        when(schemeRegularityRepository.getRegionWiseWaterQuantityByLgd(101, START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.ChildRegionWaterQuantityMetrics(401, null, "LGD-A", 120L, 10)
                ));

        var response = service.getRegionWiseWaterQuantityByLgd(101, START, END);

        assertThat(response.getParentLgdId()).isEqualTo(101);
        assertThat(response.getChildRegionCount()).isEqualTo(1);
        assertThat(response.getChildRegions().getFirst().getWaterQuantity()).isEqualTo(120L);
    }

    @Test
    void getRegionWiseWaterQuantityByDepartment_valid_mapsChildMetrics() {
        when(schemeRegularityRepository.getDepartmentLevel(201)).thenReturn(2);
        when(schemeRegularityRepository.getRegionWiseWaterQuantityByDepartment(201, START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.ChildRegionWaterQuantityMetrics(null, 501, "Dept-A", 150L, 11)
                ));

        var response = service.getRegionWiseWaterQuantityByDepartment(201, START, END);

        assertThat(response.getParentDepartmentId()).isEqualTo(201);
        assertThat(response.getChildRegionCount()).isEqualTo(1);
        assertThat(response.getChildRegions().getFirst().getDepartmentId()).isEqualTo(501);
    }

    @Test
    void getPeriodicWaterQuantityByDepartment_validMapsMetrics() {
        when(schemeRegularityRepository.getPeriodicWaterQuantityByDepartment(201, START, END, PeriodScale.DAY))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.PeriodicWaterQuantityMetrics(
                                START, START, "2026-01-01", new BigDecimal("22.1250"), 44)
                ));

        PeriodicWaterQuantityResponse response =
                service.getPeriodicWaterQuantityByDepartment(201, START, END, PeriodScale.DAY);

        assertThat(response.getDepartmentId()).isEqualTo(201);
        assertThat(response.getPeriodCount()).isEqualTo(1);
        assertThat(response.getMetrics().getFirst().getAverageWaterQuantity()).isEqualByComparingTo("22.1250");
    }

    @Test
    void getOutageReasonSchemeCountByDepartment_mapsReasonAndChildRows() {
        when(schemeRegularityRepository.getDepartmentLevel(201)).thenReturn(2);
        when(schemeRegularityRepository.getOutageReasonSchemeCountByDepartment(201, START, END))
                .thenReturn(List.of(new SchemeRegularityRepository.OutageReasonSchemeCount(1, 3)));
        when(schemeRegularityRepository.getChildRegionsByDepartment(201))
                .thenReturn(List.of(new SchemeRegularityRepository.ChildRegionRef(null, 501, "Dept-A")));
        when(schemeRegularityRepository.getChildOutageReasonSchemeCountByDepartment(201, START, END))
                .thenReturn(List.of(new SchemeRegularityRepository.ChildRegionOutageReasonSchemeCount(
                        null, 501, 2, 4
                )));

        OutageReasonSchemeCountResponse response =
                service.getOutageReasonSchemeCountByDepartment(201, START, END);

        assertThat(response.getDepartmentId()).isEqualTo(201);
        assertThat(response.getOutageReasonSchemeCount()).containsEntry("draught", 3);
        assertThat(response.getOutageReasonSchemeCount()).containsEntry("no_electricity", 0);
        assertThat(response.getChildRegions()).hasSize(1);
        assertThat(response.getChildRegions().getFirst().getOutageReasonSchemeCount()).containsEntry("no_electricity", 4);
    }

    @Test
    void getOutageReasonSchemeCountByUser_returnsReasonCountsWithMissingKeysAsZero() {
        when(schemeRegularityRepository.getOutageReasonSchemeCountByUser(11, START, END))
                .thenReturn(List.of(new SchemeRegularityRepository.OutageReasonSchemeCount(3, 2)));
        when(schemeRegularityRepository.getDailyOutageReasonSchemeCountByUser(11, START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.DailyOutageReasonSchemeCount(START, 2, 1),
                        new SchemeRegularityRepository.DailyOutageReasonSchemeCount(START.plusDays(1), 3, 2)
                ));
        when(schemeRegularityRepository.getSchemeCountByUser(11)).thenReturn(2);

        UserOutageReasonSchemeCountResponse response =
                service.getOutageReasonSchemeCountByUser(11, START, END);

        assertThat(response.getUserId()).isEqualTo(11);
        assertThat(response.getSchemeCount()).isEqualTo(2);
        assertThat(response.getOutageReasonSchemeCount())
                .containsEntry("draught", 0)
                .containsEntry("no_electricity", 0)
                .containsEntry("motor_burnt", 2);
        assertThat(response.getDailyOutageReasonDistribution()).hasSize(3);
        assertThat(response.getDailyOutageReasonDistribution().get(0).getOutageReasonSchemeCount())
                .containsEntry("no_electricity", 1);
        assertThat(response.getDailyOutageReasonDistribution().get(1).getOutageReasonSchemeCount())
                .containsEntry("motor_burnt", 2);
        assertThat(response.getDailyOutageReasonDistribution().get(2).getOutageReasonSchemeCount())
                .containsEntry("draught", 0)
                .containsEntry("no_electricity", 0)
                .containsEntry("motor_burnt", 0);
    }

    @Test
    void getSubmissionStatusByUser_returnsCompliantAndAnomalousCounts() {
        when(schemeRegularityRepository.getSchemeCountByUser(11)).thenReturn(2);
        when(schemeRegularityRepository.getSubmissionStatusCountByUser(11, START, END))
                .thenReturn(new SchemeRegularityRepository.SubmissionStatusCount(4, 1));
        when(schemeRegularityRepository.getDailySubmissionSchemeCountByUser(11, START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.DailySubmissionSchemeCount(START, 1),
                        new SchemeRegularityRepository.DailySubmissionSchemeCount(START.plusDays(2), 2)
                ));

        UserSubmissionStatusResponse response = service.getSubmissionStatusByUser(11, START, END);

        assertThat(response.getUserId()).isEqualTo(11);
        assertThat(response.getSchemeCount()).isEqualTo(2);
        assertThat(response.getCompliantSubmissionCount()).isEqualTo(4);
        assertThat(response.getAnomalousSubmissionCount()).isEqualTo(1);
        assertThat(response.getDailySubmissionSchemeDistribution()).hasSize(3);
        assertThat(response.getDailySubmissionSchemeDistribution().get(0).getSubmittedSchemeCount()).isEqualTo(1);
        assertThat(response.getDailySubmissionSchemeDistribution().get(1).getSubmittedSchemeCount()).isEqualTo(0);
        assertThat(response.getDailySubmissionSchemeDistribution().get(2).getSubmittedSchemeCount()).isEqualTo(2);
    }

    @Test
    void getOutageReasonSchemeCountByUser_withInvalidUser_throwsBadRequest() {
        assertThatThrownBy(() -> service.getOutageReasonSchemeCountByUser(0, START, END))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user_id must be a positive integer");
    }

    @Test
    void getSchemeStatusCountByDepartment_handlesNullCountsAsZero() {
        when(schemeRegularityRepository.getSchemeStatusCountByDepartment(201))
                .thenReturn(new SchemeRegularityRepository.SchemeStatusCount(4, null));

        Map<String, Integer> result = service.getSchemeStatusCountByDepartment(201);

        assertThat(result)
                .containsEntry("active_schemes_count", 4)
                .containsEntry("inactive_schemes_count", 0);
    }

    @Test
    void refreshNationalDashboard_computesAndWritesCache() throws Exception {
        mockRedisValueOps();
        String key = ":national:dashboard:start:2026-01-01:end:2026-01-03:v1";

        when(schemeRegularityRepository.getAverageWaterSupplyPerNation(START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.ChildRegionWaterSupplyMetrics(
                                1, "mp", null, null, "Madhya Pradesh", 120, 64000L, 5, new BigDecimal("12800.0000"))
                ));
        when(schemeRegularityRepository.getStateWiseRegularityMetrics(START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.StateSchemeRegularityMetrics(
                                1, "mp", "Madhya Pradesh", 5, 12)
                ));
        when(schemeRegularityRepository.getStateWiseReadingSubmissionMetrics(START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.StateReadingSubmissionMetrics(
                                1, "mp", "Madhya Pradesh", 5, 10)
                ));
        when(schemeRegularityRepository.getOverallOutageReasonSchemeCount(START, END))
                .thenReturn(List.of(
                        new SchemeRegularityRepository.OutageReasonSchemeCount(1, 3)
                ));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        NationalDashboardResponse response = service.refreshNationalDashboard(START, END);

        assertThat(response.getDaysInRange()).isEqualTo(3);
        assertThat(response.getStateWiseQuantityPerformance()).hasSize(1);
        assertThat(response.getStateWiseRegularity()).hasSize(1);
        assertThat(response.getStateWiseReadingSubmissionRate()).hasSize(1);
        verify(valueOperations, times(1)).set(eq(key), eq("{json}"), eq(Duration.ofHours(24)));
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
