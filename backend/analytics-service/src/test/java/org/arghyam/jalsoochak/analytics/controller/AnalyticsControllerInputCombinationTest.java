package org.arghyam.jalsoochak.analytics.controller;

import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.AverageWaterSupplyResponse;
import org.arghyam.jalsoochak.analytics.dto.response.NonSubmissionReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.OutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.PeriodicWaterQuantityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;
import org.arghyam.jalsoochak.analytics.dto.response.RegionWiseWaterQuantityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.SchemeRegularityListResponse;
import org.arghyam.jalsoochak.analytics.dto.response.SchemeStatusAndTopReportingResponse;
import org.arghyam.jalsoochak.analytics.dto.response.TenantDetailsResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserNonSubmissionReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserOutageReasonSchemeCountResponse;
import org.arghyam.jalsoochak.analytics.dto.response.SubmissionStatusSummaryResponse;
import org.arghyam.jalsoochak.analytics.dto.response.UserSubmissionStatusResponse;
import org.arghyam.jalsoochak.analytics.enums.PeriodScale;
import org.arghyam.jalsoochak.analytics.exception.GlobalExceptionHandler;
import org.arghyam.jalsoochak.analytics.repository.DimSchemeRepository;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.FactEscalationRepository;
import org.arghyam.jalsoochak.analytics.repository.FactMeterReadingRepository;
import org.arghyam.jalsoochak.analytics.repository.FactSchemePerformanceRepository;
import org.arghyam.jalsoochak.analytics.service.DateDimensionService;
import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import org.arghyam.jalsoochak.analytics.service.TenantDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerInputCombinationTest {

    private static final String BASE = "/api/v1/analytics";
    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 1, 31);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DimTenantRepository dimTenantRepository;
    @MockBean
    private DimSchemeRepository dimSchemeRepository;
    @MockBean
    private FactMeterReadingRepository meterReadingRepository;
    @MockBean
    private FactEscalationRepository escalationRepository;
    @MockBean
    private FactSchemePerformanceRepository schemePerformanceRepository;
    @MockBean
    private DateDimensionService dateDimensionService;
    @MockBean
    private TenantDetailsService tenantDetailsService;
    @MockBean
    private SchemeRegularityService schemeRegularityService;

    @Test
    void getTenantDetails_withParentLgdId_routesToLgdServices() throws Exception {
        when(tenantDetailsService.getTenantDetails(10, 101)).thenReturn(TenantDetailsResponse.builder().tenantId(10).build());
        when(schemeRegularityService.getAverageSchemeRegularity(eq(101), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(averageRegularityResponse());
        when(schemeRegularityService.getReadingSubmissionRateByLgd(eq(101), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(readingSubmissionResponse());

        mockMvc.perform(get(BASE + "/tenant_data")
                        .param("tenant_id", "10")
                        .param("parent_lgd_id", "101"))
                .andExpect(status().isOk());

        verify(tenantDetailsService, times(1)).getTenantDetails(10, 101);
        verify(schemeRegularityService, times(1)).getAverageSchemeRegularity(eq(101), any(LocalDate.class), any(LocalDate.class));
        verify(schemeRegularityService, times(1)).getReadingSubmissionRateByLgd(eq(101), any(LocalDate.class), any(LocalDate.class));
        verify(tenantDetailsService, never()).getTenantDetailsByParentDepartment(any(), any());
    }

    @Test
    void getTenantDetails_withParentDepartmentId_routesToDepartmentServices() throws Exception {
        when(tenantDetailsService.getTenantDetailsByParentDepartment(10, 201))
                .thenReturn(TenantDetailsResponse.builder().tenantId(10).build());
        when(schemeRegularityService.getAverageSchemeRegularityByDepartment(eq(201), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(averageRegularityResponse());
        when(schemeRegularityService.getReadingSubmissionRateByDepartment(eq(201), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(readingSubmissionResponse());

        mockMvc.perform(get(BASE + "/tenant_data")
                        .param("tenant_id", "10")
                        .param("parent_department_id", "201"))
                .andExpect(status().isOk());

        verify(tenantDetailsService, times(1)).getTenantDetailsByParentDepartment(10, 201);
        verify(schemeRegularityService, times(1)).getAverageSchemeRegularityByDepartment(eq(201), any(LocalDate.class), any(LocalDate.class));
        verify(schemeRegularityService, times(1)).getReadingSubmissionRateByDepartment(eq(201), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getTenantDetails_withBothParentIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/tenant_data")
                        .param("tenant_id", "10")
                        .param("parent_lgd_id", "101")
                        .param("parent_department_id", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("either parent_lgd_id or parent_department_id, not both")));

        verifyNoInteractions(tenantDetailsService, schemeRegularityService);
    }

    @Test
    void getTenantDetails_withNoParentIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/tenant_data")
                        .param("tenant_id", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Provide either parent_lgd_id or parent_department_id")));

        verifyNoInteractions(tenantDetailsService, schemeRegularityService);
    }

    @Test
    void getSchemes_withTenantId_routesToTenantFilter() throws Exception {
        when(dimSchemeRepository.findByTenantId(10)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/schemes").param("tenantId", "10"))
                .andExpect(status().isOk());

        verify(dimSchemeRepository, times(1)).findByTenantId(10);
        verify(dimSchemeRepository, never()).findAll();
    }

    @Test
    void getSchemes_withoutTenantId_returnsAll() throws Exception {
        when(dimSchemeRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/schemes"))
                .andExpect(status().isOk());

        verify(dimSchemeRepository, times(1)).findAll();
        verify(dimSchemeRepository, never()).findByTenantId(any());
    }

    @Test
    void getMeterReadings_schemeAndDates_routesToSchemeDateBranch() throws Exception {
        when(meterReadingRepository.findBySchemeIdAndReadingDateBetween(11, START, END)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/meter-readings")
                        .param("schemeId", "11")
                        .param("startDate", START.toString())
                        .param("endDate", END.toString()))
                .andExpect(status().isOk());

        verify(meterReadingRepository, times(1)).findBySchemeIdAndReadingDateBetween(11, START, END);
    }

    @Test
    void getMeterReadings_tenantAndDates_routesToTenantDateBranch() throws Exception {
        when(meterReadingRepository.findByTenantIdAndReadingDateBetween(12, START, END)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/meter-readings")
                        .param("tenantId", "12")
                        .param("startDate", START.toString())
                        .param("endDate", END.toString()))
                .andExpect(status().isOk());

        verify(meterReadingRepository, times(1)).findByTenantIdAndReadingDateBetween(12, START, END);
    }

    @Test
    void getMeterReadings_schemeOnly_routesToSchemeBranch() throws Exception {
        when(meterReadingRepository.findBySchemeId(13)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/meter-readings").param("schemeId", "13"))
                .andExpect(status().isOk());

        verify(meterReadingRepository, times(1)).findBySchemeId(13);
    }

    @Test
    void getMeterReadings_tenantOnly_routesToTenantBranch() throws Exception {
        when(meterReadingRepository.findByTenantId(14)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/meter-readings").param("tenantId", "14"))
                .andExpect(status().isOk());

        verify(meterReadingRepository, times(1)).findByTenantId(14);
    }

    @Test
    void getMeterReadings_noFilters_returnsAll() throws Exception {
        when(meterReadingRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/meter-readings"))
                .andExpect(status().isOk());

        verify(meterReadingRepository, times(1)).findAll();
    }

    @ParameterizedTest
    @MethodSource("regionWiseValidRoutes")
    void getWaterQuantityRegionWise_validRoutes(String paramName, String paramValue, boolean lgdRoute) throws Exception {
        if (lgdRoute) {
            when(schemeRegularityService.getRegionWiseWaterQuantityByLgd(Integer.parseInt(paramValue), START, END))
                    .thenReturn(regionWiseWaterQuantityResponse());
        } else {
            when(schemeRegularityService.getRegionWiseWaterQuantityByDepartment(Integer.parseInt(paramValue), START, END))
                    .thenReturn(regionWiseWaterQuantityResponse());
        }

        mockMvc.perform(get(BASE + "/water-quantity/region-wise")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param(paramName, paramValue))
                .andExpect(status().isOk()); 

        if (lgdRoute) {
            verify(schemeRegularityService, times(1))
                    .getRegionWiseWaterQuantityByLgd(Integer.parseInt(paramValue), START, END);
        } else {
            verify(schemeRegularityService, times(1))
                    .getRegionWiseWaterQuantityByDepartment(Integer.parseInt(paramValue), START, END);
        }
    }

    @Test
    void getWaterQuantityRegionWise_withBothParentIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/water-quantity/region-wise")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101")
                        .param("parent_department_id", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWaterQuantityRegionWise_withNoParentId_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/water-quantity/region-wise")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString()))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("periodicValidRoutes")
    void getPeriodicWaterQuantity_validRoutes(String idParam, String idValue, String scale, boolean lgdRoute) throws Exception {
        if (lgdRoute) {
            when(schemeRegularityService.getPeriodicWaterQuantityByLgdId(
                    Integer.parseInt(idValue), START, END, PeriodScale.fromValue(scale)))
                    .thenReturn(periodicWaterQuantityResponse());
        } else {
            when(schemeRegularityService.getPeriodicWaterQuantityByDepartment(
                    Integer.parseInt(idValue), START, END, PeriodScale.fromValue(scale)))
                    .thenReturn(periodicWaterQuantityResponse());
        }

        mockMvc.perform(get(BASE + "/water-quantity/periodic")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("scale", scale)
                        .param(idParam, idValue))
                .andExpect(status().isOk());
    }

    @Test
    void getPeriodicWaterQuantity_withBothIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/water-quantity/periodic")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("scale", "day")
                        .param("lgd_id", "101")
                        .param("department_id", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPeriodicWaterQuantity_withNoId_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/water-quantity/periodic")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("scale", "day"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPeriodicWaterQuantity_withUnsupportedScale_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/water-quantity/periodic")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("scale", "year")
                        .param("lgd_id", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported scale")));
    }

    @ParameterizedTest
    @MethodSource("outageValidRoutes")
    void getOutageReasons_validRoutes(String paramName, String paramValue, boolean lgdRoute) throws Exception {
        if (lgdRoute) {
            when(schemeRegularityService.getOutageReasonSchemeCountByLgd(Integer.parseInt(paramValue), START, END))
                    .thenReturn(outageReasonResponse());
        } else {
            when(schemeRegularityService.getOutageReasonSchemeCountByDepartment(Integer.parseInt(paramValue), START, END))
                    .thenReturn(outageReasonResponse());
        }

        mockMvc.perform(get(BASE + "/outage-reasons")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param(paramName, paramValue))
                .andExpect(status().isOk());
    }

    @Test
    void getOutageReasons_withBothIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/outage-reasons")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101")
                        .param("parent_department_id", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOutageReasons_withNoId_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/outage-reasons")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOutageReasonsByUser_validRequest_routesToUserService() throws Exception {
        when(schemeRegularityService.getOutageReasonSchemeCountByUserUuid(USER_UUID, START, END))
                .thenReturn(userOutageReasonResponse());

        mockMvc.perform(get(BASE + "/outage-reasons/user")
                        .principal(buildJwtAuthentication())
                        .param("start_date", START.toString())
                        .param("end_date", END.toString()))
                .andExpect(status().isOk());

        verify(schemeRegularityService, times(1)).getOutageReasonSchemeCountByUserUuid(USER_UUID, START, END);
    }

    @ParameterizedTest
    @MethodSource("nonSubmissionValidRoutes")
    void getNonSubmissionReasons_validRoutes(String paramName, String paramValue, boolean lgdRoute) throws Exception {
        if (lgdRoute) {
            when(schemeRegularityService.getNonSubmissionReasonSchemeCountByLgd(Integer.parseInt(paramValue), START, END))
                    .thenReturn(nonSubmissionReasonResponse());
        } else {
            when(schemeRegularityService.getNonSubmissionReasonSchemeCountByDepartment(Integer.parseInt(paramValue), START, END))
                    .thenReturn(nonSubmissionReasonResponse());
        }

        mockMvc.perform(get(BASE + "/non-submission-reasons")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param(paramName, paramValue))
                .andExpect(status().isOk());
    }

    @Test
    void getNonSubmissionReasons_withBothIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/non-submission-reasons")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101")
                        .param("parent_department_id", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNonSubmissionReasons_withNoId_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/non-submission-reasons")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNonSubmissionReasonsByUser_validRequest_routesToUserService() throws Exception {
        when(schemeRegularityService.getNonSubmissionReasonSchemeCountByUserUuid(USER_UUID, START, END))
                .thenReturn(userNonSubmissionReasonResponse());

        mockMvc.perform(get(BASE + "/non-submission-reasons/user")
                        .principal(buildJwtAuthentication())
                        .param("start_date", START.toString())
                        .param("end_date", END.toString()))
                .andExpect(status().isOk());

        verify(schemeRegularityService, times(1)).getNonSubmissionReasonSchemeCountByUserUuid(USER_UUID, START, END);
    }

    @Test
    void getSubmissionStatusByUser_validRequest_routesToUserService() throws Exception {
        when(schemeRegularityService.getSubmissionStatusByUserUuid(USER_UUID, START, END))
                .thenReturn(userSubmissionStatusResponse());

        mockMvc.perform(get(BASE + "/submission-status/user")
                        .principal(buildJwtAuthentication())
                        .param("start_date", START.toString())
                        .param("end_date", END.toString()))
                .andExpect(status().isOk());

        verify(schemeRegularityService, times(1)).getSubmissionStatusByUserUuid(USER_UUID, START, END);
    }

    @Test
    void getSubmissionStatusSummary_withLgdId_routesToLgdService() throws Exception {
        when(schemeRegularityService.getSubmissionStatusSummaryByLgd(100, START, END))
                .thenReturn(SubmissionStatusSummaryResponse.builder()
                        .schemeCount(2)
                        .compliantSubmissionCount(5)
                        .anomalousSubmissionCount(0)
                        .build());

        mockMvc.perform(get(BASE + "/submission-status")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("lgd_id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeCount").value(2))
                .andExpect(jsonPath("$.compliantSubmissionCount").value(5))
                .andExpect(jsonPath("$.anomalousSubmissionCount").value(0));

        verify(schemeRegularityService, times(1)).getSubmissionStatusSummaryByLgd(100, START, END);
    }

    @Test
    void getSubmissionStatusSummary_withDepartmentId_routesToDepartmentService() throws Exception {
        when(schemeRegularityService.getSubmissionStatusSummaryByDepartment(200, START, END))
                .thenReturn(SubmissionStatusSummaryResponse.builder()
                        .schemeCount(2)
                        .compliantSubmissionCount(5)
                        .anomalousSubmissionCount(0)
                        .build());

        mockMvc.perform(get(BASE + "/submission-status")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("department_id", "200"))
                .andExpect(status().isOk());

        verify(schemeRegularityService, times(1)).getSubmissionStatusSummaryByDepartment(200, START, END);
    }

    @Test
    void getSubmissionStatusSummary_withBothIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/submission-status")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("lgd_id", "100")
                        .param("department_id", "200"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSubmissionStatusSummary_withNoScopeId_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/submission-status")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString()))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("schemeStatusValidRoutes")
    void getSchemeStatusCount_validRoutes(String idParam, String idValue, boolean lgdRoute) throws Exception {
        if (lgdRoute) {
            when(schemeRegularityService.getSchemeStatusCountByLgd(Integer.parseInt(idValue)))
                    .thenReturn(Map.of("active_schemes_count", 5, "inactive_schemes_count", 1));
        } else {
            when(schemeRegularityService.getSchemeStatusCountByDepartment(Integer.parseInt(idValue)))
                    .thenReturn(Map.of("active_schemes_count", 5, "inactive_schemes_count", 1));
        }

        mockMvc.perform(get(BASE + "/schemes/status-count").param(idParam, idValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active_schemes_count").value(5))
                .andExpect(jsonPath("$.inactive_schemes_count").value(1));
    }

    @Test
    void getSchemeStatusCount_withBothIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/schemes/status-count")
                        .param("lgd_id", "101")
                        .param("department_id", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSchemeStatusCount_withNoId_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/schemes/status-count"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSchemesDashboard_withParentLgdId_returnsParentLgdCName() throws Exception {
        when(schemeRegularityService.getSchemeStatusAndTopReportingByLgd(101, START, END, 5))
                .thenReturn(SchemeStatusAndTopReportingResponse.builder()
                        .parentLgdId(101)
                        .parentLgdCName("Parent")
                        .parentLgdTitle("Parent LGD")
                        .activeSchemeCount(1)
                        .inactiveSchemeCount(1)
                        .topSchemeCount(1)
                        .topSchemes(List.of(SchemeStatusAndTopReportingResponse.TopReportingScheme.builder()
                                .schemeId(1)
                                .schemeName("Scheme A")
                                .statusCode(1)
                                .status("active")
                                .submissionDays(10)
                                .reportingRate(BigDecimal.valueOf(0.5))
                                .totalWaterSupplied(150L)
                                .immediateParentLgdId(100)
                                .immediateParentLgdCName("Parent")
                                .immediateParentLgdTitle("Parent LGD")
                                .build()))
                        .build());

        mockMvc.perform(get(BASE + "/schemes/dashboard")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101")
                        .param("scheme_count", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentLgdId").value(101))
                .andExpect(jsonPath("$.parentLgdCName").value("Parent"))
                .andExpect(jsonPath("$.parentLgdTitle").value("Parent LGD"))
                .andExpect(jsonPath("$.topSchemes[0].totalWaterSupplied").value(150))
                .andExpect(jsonPath("$.topSchemes[0].immediateParentLgdId").value(100))
                .andExpect(jsonPath("$.topSchemes[0].immediateParentLgdCName").value("Parent"))
                .andExpect(jsonPath("$.topSchemes[0].immediateParentLgdTitle").value("Parent LGD"));
    }

    @Test
    void getSchemesDashboard_withParentDepartmentId_returnsParentDepartmentCName() throws Exception {
        when(schemeRegularityService.getSchemeStatusAndTopReportingByDepartment(201, START, END, 5))
                .thenReturn(SchemeStatusAndTopReportingResponse.builder()
                        .parentDepartmentId(201)
                        .parentDepartmentCName("Parent Dept")
                        .parentDepartmentTitle("Parent Dept")
                        .activeSchemeCount(1)
                        .inactiveSchemeCount(1)
                        .topSchemeCount(1)
                        .topSchemes(List.of(SchemeStatusAndTopReportingResponse.TopReportingScheme.builder()
                                .schemeId(2)
                                .schemeName("Scheme B")
                                .statusCode(1)
                                .status("active")
                                .submissionDays(8)
                                .reportingRate(BigDecimal.valueOf(0.4))
                                .totalWaterSupplied(80L)
                                .immediateParentDepartmentId(200)
                                .immediateParentDepartmentCName("Parent Dept")
                                .immediateParentDepartmentTitle("Parent Dept")
                                .build()))
                        .build());

        mockMvc.perform(get(BASE + "/schemes/dashboard")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_department_id", "201")
                        .param("scheme_count", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentDepartmentId").value(201))
                .andExpect(jsonPath("$.parentDepartmentCName").value("Parent Dept"))
                .andExpect(jsonPath("$.parentDepartmentTitle").value("Parent Dept"))
                .andExpect(jsonPath("$.topSchemes[0].totalWaterSupplied").value(80))
                .andExpect(jsonPath("$.topSchemes[0].immediateParentDepartmentId").value(200))
                .andExpect(jsonPath("$.topSchemes[0].immediateParentDepartmentCName").value("Parent Dept"))
                .andExpect(jsonPath("$.topSchemes[0].immediateParentDepartmentTitle").value("Parent Dept"));
    }

    @Test
    void getSchemeRegionReport_withParentLgdId_routesToLgdService() throws Exception {
        when(schemeRegularityService.getSchemeRegionReportByLgd(101, START, END, null, null))
                .thenReturn(SchemeRegularityListResponse.builder()
                        .parentLgdId(101)
                        .totalSchemeCount(1)
                        .activeSchemeCount(1)
                        .inactiveSchemeCount(0)
                        .schemeCountInResponse(1)
                        .schemes(List.of(
                                SchemeRegularityListResponse.SchemeMetrics.builder()
                                        .schemeId(1)
                                        .schemeName("Scheme A")
                                        .statusCode(1)
                                        .status("active")
                                        .supplyDays(2)
                                        .averageRegularity(BigDecimal.valueOf(0.6667))
                                        .submissionDays(3)
                                        .submissionRate(BigDecimal.valueOf(1.0000))
                                        .build()))
                        .build());

        mockMvc.perform(get(BASE + "/schemes/region-report")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentLgdId").value(101))
                .andExpect(jsonPath("$.schemes[0].schemeId").value(1));

        verify(schemeRegularityService, times(1))
                .getSchemeRegionReportByLgd(101, START, END, null, null);
        verify(schemeRegularityService, never()).getSchemeRegionReportByDepartment(any(), any(), any(), any(), any());
    }

    @Test
    void getSchemeRegionReport_withParentDepartmentId_routesToDepartmentService() throws Exception {
        when(schemeRegularityService.getSchemeRegionReportByDepartment(201, START, END, null, null))
                .thenReturn(SchemeRegularityListResponse.builder()
                        .parentDepartmentId(201)
                        .totalSchemeCount(1)
                        .activeSchemeCount(0)
                        .inactiveSchemeCount(1)
                        .schemeCountInResponse(1)
                        .schemes(List.of(
                                SchemeRegularityListResponse.SchemeMetrics.builder()
                                        .schemeId(2)
                                        .schemeName("Scheme B")
                                        .statusCode(0)
                                        .status("inactive")
                                        .supplyDays(0)
                                        .averageRegularity(BigDecimal.ZERO)
                                        .submissionDays(1)
                                        .submissionRate(BigDecimal.valueOf(0.3333))
                                        .build()))
                        .build());

        mockMvc.perform(get(BASE + "/schemes/region-report")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_department_id", "201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentDepartmentId").value(201))
                .andExpect(jsonPath("$.schemes[0].schemeId").value(2));

        verify(schemeRegularityService, times(1))
                .getSchemeRegionReportByDepartment(201, START, END, null, null);
        verify(schemeRegularityService, never()).getSchemeRegionReportByLgd(any(), any(), any(), any(), any());
    }

    @Test
    void getSchemeRegionReport_withBothParentIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/schemes/region-report")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101")
                        .param("parent_department_id", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSchemeRegionReport_withPaginationParams_passesPaginationToService() throws Exception {
        when(schemeRegularityService.getSchemeRegionReportByLgd(101, START, END, 2, 1))
                .thenReturn(SchemeRegularityListResponse.builder()
                        .parentLgdId(101)
                        .schemeCountInResponse(0)
                        .schemes(List.of())
                        .build());

        mockMvc.perform(get(BASE + "/schemes/region-report")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101")
                        .param("page_number", "2")
                        .param("count", "1"))
                .andExpect(status().isOk());

        verify(schemeRegularityService, times(1))
                .getSchemeRegionReportByLgd(101, START, END, 2, 1);
    }

    @Test
    void getSchemeRegionReport_withCsvOutputFormat_returnsCsvAttachmentForParentLgd() throws Exception {
        when(schemeRegularityService.getSchemeRegionReportByLgd(101, START, END, null, null))
                .thenReturn(SchemeRegularityListResponse.builder()
                        .parentLgdId(101)
                        .parentLgdCName("Parent LGD Name")
                        .schemes(List.of(
                                SchemeRegularityListResponse.SchemeMetrics.builder()
                                        .schemeId(1)
                                        .schemeName("Scheme A")
                                        .statusCode(1)
                                        .status("active")
                                        .supplyDays(2)
                                        .averageRegularity(BigDecimal.valueOf(0.6667))
                                        .submissionDays(3)
                                        .submissionRate(BigDecimal.valueOf(1.0000))
                                        .build()))
                        .build());

        mockMvc.perform(get(BASE + "/schemes/region-report")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101")
                        .param("output_format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"scheme-region-report_parent_lgd_name_2026-01-01_to_2026-01-31.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(startsWith(
                        "scheme_id,scheme_name,status_code,status,supply_days,average_regularity,submission_days,submission_rate")))
                .andExpect(content().string(containsString("1,Scheme A,1,active,2,0.6667,3,1.0")));
    }

    @Test
    void getSchemeRegionReport_withCsvOutputFormat_returnsCsvAttachmentForParentDepartment() throws Exception {
        when(schemeRegularityService.getSchemeRegionReportByDepartment(201, START, END, null, null))
                .thenReturn(SchemeRegularityListResponse.builder()
                        .parentDepartmentId(201)
                        .parentDepartmentCName("Department (HQ)")
                        .schemes(List.of(
                                SchemeRegularityListResponse.SchemeMetrics.builder()
                                        .schemeId(2)
                                        .schemeName("Scheme, B")
                                        .statusCode(0)
                                        .status("inactive")
                                        .supplyDays(0)
                                        .averageRegularity(BigDecimal.ZERO)
                                        .submissionDays(1)
                                        .submissionRate(BigDecimal.valueOf(0.3333))
                                        .build()))
                        .build());

        mockMvc.perform(get(BASE + "/schemes/region-report")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_department_id", "201")
                        .param("output_format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"scheme-region-report_department_hq_2026-01-01_to_2026-01-31.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("2,\"Scheme, B\",0,inactive,0,0,1,0.3333")));
    }

    @Test
    void getSchemeRegionReport_withoutCsvOutputFormat_behavesAsJson() throws Exception {
        when(schemeRegularityService.getSchemeRegionReportByLgd(101, START, END, null, null))
                .thenReturn(SchemeRegularityListResponse.builder()
                        .parentLgdId(101)
                        .schemes(List.of())
                        .build());

        mockMvc.perform(get(BASE + "/schemes/region-report")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101")
                        .param("output_format", "json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentLgdId").value(101))
                .andExpect(header().doesNotExist("Content-Disposition"));
    }

    @Test
    void getEscalations_tenantAndResolution_routesToTenantResolutionBranch() throws Exception {
        when(escalationRepository.findByTenantIdAndResolutionStatus(10, 1)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/escalations")
                        .param("tenantId", "10")
                        .param("resolutionStatus", "1"))
                .andExpect(status().isOk());

        verify(escalationRepository, times(1)).findByTenantIdAndResolutionStatus(10, 1);
        verify(escalationRepository, never()).findBySchemeId(any());
    }

    @Test
    void getEscalations_schemeProvided_routesToSchemeBranch() throws Exception {
        when(escalationRepository.findBySchemeId(200)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/escalations").param("schemeId", "200"))
                .andExpect(status().isOk());

        verify(escalationRepository, times(1)).findBySchemeId(200);
    }

    @Test
    void getEscalations_tenantOnly_routesToTenantBranch() throws Exception {
        when(escalationRepository.findByTenantId(10)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/escalations").param("tenantId", "10"))
                .andExpect(status().isOk());

        verify(escalationRepository, times(1)).findByTenantId(10);
    }

    @Test
    void getEscalations_noFilters_returnsAll() throws Exception {
        when(escalationRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/escalations"))
                .andExpect(status().isOk());

        verify(escalationRepository, times(1)).findAll();
    }

    @Test
    void getSchemePerformance_schemePreferredOverTenant() throws Exception {
        when(schemePerformanceRepository.findBySchemeId(300)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/scheme-performance")
                        .param("tenantId", "10")
                        .param("schemeId", "300"))
                .andExpect(status().isOk());

        verify(schemePerformanceRepository, times(1)).findBySchemeId(300);
        verify(schemePerformanceRepository, never()).findByTenantId(any());
    }

    @Test
    void getSchemePerformance_tenantOnly_routesToTenantBranch() throws Exception {
        when(schemePerformanceRepository.findByTenantId(10)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/scheme-performance").param("tenantId", "10"))
                .andExpect(status().isOk());

        verify(schemePerformanceRepository, times(1)).findByTenantId(10);
    }

    @Test
    void getSchemePerformance_noFilters_returnsAll() throws Exception {
        when(schemePerformanceRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/scheme-performance"))
                .andExpect(status().isOk());

        verify(schemePerformanceRepository, times(1)).findAll();
    }

    @ParameterizedTest
    @MethodSource("averageRegularityValidRoutes")
    void getAverageSchemeRegularity_validScopeAndIdCombinations(
            String scope,
            String idParam,
            String idValue,
            int expectedServiceCall) throws Exception {
        Mockito.reset(schemeRegularityService);
        when(schemeRegularityService.getAverageSchemeRegularity(any(), any(), any())).thenReturn(averageRegularityResponse());
        when(schemeRegularityService.getAverageSchemeRegularityByDepartment(any(), any(), any())).thenReturn(averageRegularityResponse());
        when(schemeRegularityService.getAverageSchemeRegularityForChildRegions(any(), any(), any())).thenReturn(averageRegularityResponse());
        when(schemeRegularityService.getAverageSchemeRegularityByDepartmentForChildRegions(any(), any(), any()))
                .thenReturn(averageRegularityResponse());

        mockMvc.perform(get(BASE + "/scheme-regularity/average")
                        .param("scope", scope)
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param(idParam, idValue))
                .andExpect(status().isOk());

        int value = Integer.parseInt(idValue);
        if (expectedServiceCall == 1) {
            verify(schemeRegularityService, times(1)).getAverageSchemeRegularity(value, START, END);
        } else if (expectedServiceCall == 2) {
            verify(schemeRegularityService, times(1)).getAverageSchemeRegularityByDepartment(value, START, END);
        } else if (expectedServiceCall == 3) {
            verify(schemeRegularityService, times(1)).getAverageSchemeRegularityForChildRegions(value, START, END);
        } else {
            verify(schemeRegularityService, times(1))
                    .getAverageSchemeRegularityByDepartmentForChildRegions(value, START, END);
        }
    }

    @Test
    void getAverageSchemeRegularity_withBothParentIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/scheme-regularity/average")
                        .param("scope", "current")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101")
                        .param("parent_department_id", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAverageSchemeRegularity_invalidScope_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/scheme-regularity/average")
                        .param("scope", "invalid")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported scope")));
    }

    @Test
    void getAverageSchemeRegularity_serviceValidationFailure_returnsBadRequest() throws Exception {
        when(schemeRegularityService.getAverageSchemeRegularity(101, START, END))
                .thenThrow(new IllegalArgumentException("end_date must be on or after start_date"));

        mockMvc.perform(get(BASE + "/scheme-regularity/average")
                        .param("scope", "current")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("end_date must be on or after start_date")));
    }

    @ParameterizedTest
    @MethodSource("readingSubmissionValidRoutes")
    void getReadingSubmissionRate_validScopeAndIdCombinations(
            String scope,
            String idParam,
            String idValue,
            int expectedServiceCall) throws Exception {
        Mockito.reset(schemeRegularityService);
        when(schemeRegularityService.getReadingSubmissionRateByLgd(any(), any(), any())).thenReturn(readingSubmissionResponse());
        when(schemeRegularityService.getReadingSubmissionRateByDepartment(any(), any(), any())).thenReturn(readingSubmissionResponse());
        when(schemeRegularityService.getReadingSubmissionRateByLgdForChildRegions(any(), any(), any()))
                .thenReturn(readingSubmissionResponse());
        when(schemeRegularityService.getReadingSubmissionRateByDepartmentForChildRegions(any(), any(), any()))
                .thenReturn(readingSubmissionResponse());

        mockMvc.perform(get(BASE + "/reading-submission-rate")
                        .param("scope", scope)
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param(idParam, idValue))
                .andExpect(status().isOk());

        int value = Integer.parseInt(idValue);
        if (expectedServiceCall == 1) {
            verify(schemeRegularityService, times(1)).getReadingSubmissionRateByLgd(value, START, END);
        } else if (expectedServiceCall == 2) {
            verify(schemeRegularityService, times(1)).getReadingSubmissionRateByDepartment(value, START, END);
        } else if (expectedServiceCall == 3) {
            verify(schemeRegularityService, times(1)).getReadingSubmissionRateByLgdForChildRegions(value, START, END);
        } else {
            verify(schemeRegularityService, times(1))
                    .getReadingSubmissionRateByDepartmentForChildRegions(value, START, END);
        }
    }

    @Test
    void getReadingSubmissionRate_withBothParentIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/reading-submission-rate")
                        .param("scope", "current")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101")
                        .param("parent_department_id", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReadingSubmissionRate_invalidScope_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/reading-submission-rate")
                        .param("scope", "invalid")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("parent_lgd_id", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported scope")));
    }

    @ParameterizedTest
    @MethodSource("waterSupplyCombinationMatrix")
    void getAverageWaterSupplyPerRegion_combinationMatrix(
            String scope,
            String tenantId,
            String parentLgdId,
            String parentDepartmentId,
            int expectedStatus) throws Exception {
        when(schemeRegularityService.getAverageWaterSupplyPerCurrentRegion(any(), any(), any()))
                .thenReturn(averageWaterSupplyResponse());
        when(schemeRegularityService.getAverageWaterSupplyPerNation(any(), any()))
                .thenReturn(averageWaterSupplyResponse());
        when(schemeRegularityService.getAverageWaterSupplyPerCurrentRegionByLgd(any(), any(), any(), any()))
                .thenReturn(averageWaterSupplyResponse());
        when(schemeRegularityService.getAverageWaterSupplyPerCurrentRegionByDepartment(any(), any(), any(), any()))
                .thenReturn(averageWaterSupplyResponse());

        MockHttpServletRequestBuilder request = get(BASE + "/water-supply/average-per-region")
                .param("scope", scope)
                .param("start_date", START.toString())
                .param("end_date", END.toString());
        if (tenantId != null) {
            request.param("tenant_id", tenantId);
        }
        if (parentLgdId != null) {
            request.param("parent_lgd_id", parentLgdId);
        }
        if (parentDepartmentId != null) {
            request.param("parent_department_id", parentDepartmentId);
        }

        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    @Test
    void getAverageWaterSupplyPerRegion_invalidScope_returnsBadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/water-supply/average-per-region")
                        .param("scope", "invalid")
                        .param("start_date", START.toString())
                        .param("end_date", END.toString())
                        .param("tenant_id", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported scope")));
    }

    @Test
    void populateDateDimension_validDateRange_returnsOkAndCallsService() throws Exception {
        mockMvc.perform(post(BASE + "/date-dimension/populate")
                        .param("startDate", START.toString())
                        .param("endDate", END.toString()))
                .andExpect(status().isOk());

        verify(dateDimensionService, times(1)).populateDateRange(START, END);
    }

    private static Stream<Arguments> regionWiseValidRoutes() {
        return Stream.of(
                Arguments.of("parent_lgd_id", "101", true),
                Arguments.of("parent_department_id", "201", false)
        );
    }

    private static Stream<Arguments> periodicValidRoutes() {
        return Stream.of(
                Arguments.of("lgd_id", "101", "day", true),
                Arguments.of("lgd_id", "101", "week", true),
                Arguments.of("lgd_id", "101", "month", true),
                Arguments.of("department_id", "201", "day", false),
                Arguments.of("department_id", "201", "week", false),
                Arguments.of("department_id", "201", "month", false)
        );
    }

    private static Stream<Arguments> outageValidRoutes() {
        return Stream.of(
                Arguments.of("parent_lgd_id", "101", true),
                Arguments.of("parent_department_id", "201", false)
        );
    }

    private static Stream<Arguments> nonSubmissionValidRoutes() {
        return Stream.of(
                Arguments.of("parent_lgd_id", "101", true),
                Arguments.of("parent_department_id", "201", false)
        );
    }

    private static Stream<Arguments> schemeStatusValidRoutes() {
        return Stream.of(
                Arguments.of("lgd_id", "101", true),
                Arguments.of("department_id", "201", false)
        );
    }

    private static Stream<Arguments> averageRegularityValidRoutes() {
        return Stream.of(
                Arguments.of("current", "parent_lgd_id", "101", 1),
                Arguments.of("current", "parent_department_id", "201", 2),
                Arguments.of("child", "parent_lgd_id", "101", 3),
                Arguments.of("child", "parent_department_id", "201", 4)
        );
    }

    private static Stream<Arguments> readingSubmissionValidRoutes() {
        return Stream.of(
                Arguments.of("current", "parent_lgd_id", "101", 1),
                Arguments.of("current", "parent_department_id", "201", 2),
                Arguments.of("child", "parent_lgd_id", "101", 3),
                Arguments.of("child", "parent_department_id", "201", 4)
        );
    }

    private static Stream<Arguments> waterSupplyCombinationMatrix() {
        return Stream.of(
                Arguments.of("current", "10", null, null, 200),
                Arguments.of("current", null, null, null, 400),
                Arguments.of("current", "10", "101", "201", 400),
                Arguments.of("child", null, null, null, 200),
                Arguments.of("child", "10", "101", null, 200),
                Arguments.of("child", "10", null, null, 400),
                Arguments.of("child", "10", "101", "201", 400)
        );
    }

    private static JwtAuthenticationToken buildJwtAuthentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(USER_UUID.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_USER")));
    }

    private static AverageSchemeRegularityResponse averageRegularityResponse() {
        return AverageSchemeRegularityResponse.builder()
                .averageRegularity(BigDecimal.valueOf(0.75))
                .build();
    }

    private static ReadingSubmissionRateResponse readingSubmissionResponse() {
        return ReadingSubmissionRateResponse.builder()
                .readingSubmissionRate(BigDecimal.valueOf(0.84))
                .build();
    }

    private static RegionWiseWaterQuantityResponse regionWiseWaterQuantityResponse() {
        return RegionWiseWaterQuantityResponse.builder()
                .childRegionCount(0)
                .childRegions(List.of())
                .build();
    }

    private static PeriodicWaterQuantityResponse periodicWaterQuantityResponse() {
        return PeriodicWaterQuantityResponse.builder()
                .periodCount(0)
                .metrics(List.of())
                .build();
    }

    private static OutageReasonSchemeCountResponse outageReasonResponse() {
        return OutageReasonSchemeCountResponse.builder()
                .childRegionCount(0)
                .outageReasonSchemeCount(Map.of("power_failure", 0))
                .build();
    }

    private static UserOutageReasonSchemeCountResponse userOutageReasonResponse() {
        return UserOutageReasonSchemeCountResponse.builder()
                .userId(11)
                .startDate(START)
                .endDate(END)
                .schemeCount(2)
                .outageReasonSchemeCount(Map.of("draught", 1))
                .dailyOutageReasonDistribution(List.of(
                        UserOutageReasonSchemeCountResponse.DailyOutageReasonDistribution.builder()
                                .date(START)
                                .outageReasonSchemeCount(Map.of("draught", 1, "no_electricity", 0, "motor_burnt", 0))
                                .build()
                ))
                .build();
    }

    private static NonSubmissionReasonSchemeCountResponse nonSubmissionReasonResponse() {
        return NonSubmissionReasonSchemeCountResponse.builder()
                .childRegionCount(0)
                .nonSubmissionReasonSchemeCount(Map.of("operator_absent", 0))
                .build();
    }

    private static UserNonSubmissionReasonSchemeCountResponse userNonSubmissionReasonResponse() {
        return UserNonSubmissionReasonSchemeCountResponse.builder()
                .userId(11)
                .startDate(START)
                .endDate(END)
                .schemeCount(2)
                .nonSubmissionReasonSchemeCount(Map.of("app_issue", 1))
                .dailyNonSubmissionReasonDistribution(List.of(
                        UserNonSubmissionReasonSchemeCountResponse.DailyNonSubmissionReasonDistribution.builder()
                                .date(START)
                                .nonSubmissionReasonSchemeCount(Map.of("app_issue", 1))
                                .build()
                ))
                .build();
    }

    private static UserSubmissionStatusResponse userSubmissionStatusResponse() {
        return UserSubmissionStatusResponse.builder()
                .userId(11)
                .startDate(START)
                .endDate(END)
                .schemeCount(2)
                .compliantSubmissionCount(4)
                .anomalousSubmissionCount(1)
                .dailySubmissionSchemeDistribution(List.of(
                        UserSubmissionStatusResponse.DailySubmissionSchemeDistribution.builder()
                                .date(START)
                                .submittedSchemeCount(1)
                                .build()
                ))
                .build();
    }

    private static AverageWaterSupplyResponse averageWaterSupplyResponse() {
        return AverageWaterSupplyResponse.builder()
                .schemeCount(0)
                .childRegionCount(0)
                .schemes(List.of())
                .childRegions(List.of())
                .build();
    }
}
