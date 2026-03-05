package org.arghyam.jalsoochak.analytics.service.serviceImpl;

import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.AverageWaterSupplyResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;
import org.arghyam.jalsoochak.analytics.entity.DimTenant;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.SchemeRegularityRepository;
import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemeRegularityServiceImpl implements SchemeRegularityService {

    private static final Duration SCHEME_REGULARITY_CACHE_TTL = Duration.ofHours(24);
    private static final String SCHEME_REGULARITY_CACHE_PREFIX = ":scheme_regularity";
    private static final String READING_SUBMISSION_RATE_CACHE_PREFIX = ":reading_submission_rate";
    private static final String DEBUG_LOG_PATH = "/home/beehyv/Desktop/Codes/jalSoochak/JalSoochak_New/.cursor/debug.log";

    private final SchemeRegularityRepository schemeRegularityRepository;
    private final DimTenantRepository dimTenantRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public AverageSchemeRegularityResponse getAverageSchemeRegularity(Integer lgdId, LocalDate startDate, LocalDate endDate) {
        validateLgdInput(lgdId);
        validateDateRange(startDate, endDate);
        // #region agent log
        appendDebugLog(
                "H1",
                "SchemeRegularityServiceImpl:getAverageSchemeRegularity:entry",
                "Regularity request entry",
                Map.of("lgdId", lgdId, "startDate", String.valueOf(startDate), "endDate", String.valueOf(endDate)));
        // #endregion

        String cacheKey = SCHEME_REGULARITY_CACHE_PREFIX
                + ":lgd:" + lgdId
                + ":start:" + startDate
                + ":end:" + endDate;
        AverageSchemeRegularityResponse cached = readFromCache(cacheKey, AverageSchemeRegularityResponse.class);
        if (cached != null) {
            return cached;
        }

        int daysInRange = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        SchemeRegularityRepository.SchemeRegularityMetrics metrics;
        try {
            metrics = schemeRegularityRepository.getSchemeRegularityMetrics(lgdId, startDate, endDate);
        } catch (Exception ex) {
            // #region agent log
            appendDebugLog(
                    "H2",
                    "SchemeRegularityServiceImpl:getAverageSchemeRegularity:repo_exception",
                    "Regularity repository call failed",
                    Map.of("errorType", ex.getClass().getName(), "errorMessage", String.valueOf(ex.getMessage())));
            // #endregion
            throw ex;
        }
        // #region agent log
        appendDebugLog(
                "H2",
                "SchemeRegularityServiceImpl:getAverageSchemeRegularity:repo_success",
                "Regularity repository call succeeded",
                Map.of("daysInRange", daysInRange, "schemeCount", metrics.schemeCount(), "totalSupplyDays", metrics.totalSupplyDays()));
        // #endregion

        BigDecimal averageRegularity = BigDecimal.ZERO;
        if (metrics.schemeCount() > 0 && daysInRange > 0) {
            BigDecimal denominator = BigDecimal.valueOf((long) metrics.schemeCount() * daysInRange);
            averageRegularity = BigDecimal.valueOf(metrics.totalSupplyDays())
                    .divide(denominator, 4, RoundingMode.HALF_UP);
        }

        AverageSchemeRegularityResponse response = AverageSchemeRegularityResponse.builder()
                .lgdId(lgdId)
                .parentDepartmentId(null)
                .startDate(startDate)
                .endDate(endDate)
                .daysInRange(daysInRange)
                .schemeCount(metrics.schemeCount())
                .totalSupplyDays(metrics.totalSupplyDays())
                .averageRegularity(averageRegularity)
                .build();
        writeToCache(cacheKey, response);
        return response;
    }

    @Override
    public ReadingSubmissionRateResponse getReadingSubmissionRate(Integer lgdId, LocalDate startDate, LocalDate endDate) {
        validateLgdInput(lgdId);
        validateDateRange(startDate, endDate);
        // #region agent log
        appendDebugLog(
                "H3",
                "SchemeRegularityServiceImpl:getReadingSubmissionRate:entry",
                "Submission rate request entry",
                Map.of("lgdId", lgdId, "startDate", String.valueOf(startDate), "endDate", String.valueOf(endDate)));
        // #endregion

        String cacheKey = READING_SUBMISSION_RATE_CACHE_PREFIX
                + ":lgd:" + lgdId
                + ":start:" + startDate
                + ":end:" + endDate;
        ReadingSubmissionRateResponse cached = readFromCache(cacheKey, ReadingSubmissionRateResponse.class);
        if (cached != null) {
            return cached;
        }

        int daysInRange = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        SchemeRegularityRepository.SchemeRegularityMetrics metrics;
        try {
            metrics = schemeRegularityRepository.getReadingSubmissionRateMetrics(lgdId, startDate, endDate);
        } catch (Exception ex) {
            // #region agent log
            appendDebugLog(
                    "H3",
                    "SchemeRegularityServiceImpl:getReadingSubmissionRate:repo_exception",
                    "Submission rate repository call failed",
                    Map.of("errorType", ex.getClass().getName(), "errorMessage", String.valueOf(ex.getMessage())));
            // #endregion
            throw ex;
        }
        // #region agent log
        appendDebugLog(
                "H3",
                "SchemeRegularityServiceImpl:getReadingSubmissionRate:repo_success",
                "Submission rate repository call succeeded",
                Map.of("daysInRange", daysInRange, "schemeCount", metrics.schemeCount(), "totalSupplyDays", metrics.totalSupplyDays()));
        // #endregion

        BigDecimal readingSubmissionRate = BigDecimal.ZERO;
        if (metrics.schemeCount() > 0 && daysInRange > 0) {
            BigDecimal denominator = BigDecimal.valueOf((long) metrics.schemeCount() * daysInRange);
            readingSubmissionRate = BigDecimal.valueOf(metrics.totalSupplyDays())
                    .divide(denominator, 4, RoundingMode.HALF_UP);
        }

        ReadingSubmissionRateResponse response = ReadingSubmissionRateResponse.builder()
                .lgdId(lgdId)
                .parentDepartmentId(null)
                .startDate(startDate)
                .endDate(endDate)
                .daysInRange(daysInRange)
                .schemeCount(metrics.schemeCount())
                .totalSubmissionDays(metrics.totalSupplyDays())
                .readingSubmissionRate(readingSubmissionRate)
                .build();
        writeToCache(cacheKey, response);
        return response;
    }

    @Override
    public AverageSchemeRegularityResponse getAverageSchemeRegularityByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate) {
        validateDepartmentInput(parentDepartmentId);
        validateDateRange(startDate, endDate);

        String cacheKey = SCHEME_REGULARITY_CACHE_PREFIX
                + ":department:" + parentDepartmentId
                + ":start:" + startDate
                + ":end:" + endDate;
        AverageSchemeRegularityResponse cached = readFromCache(cacheKey, AverageSchemeRegularityResponse.class);
        if (cached != null) {
            return cached;
        }

        int daysInRange = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        SchemeRegularityRepository.SchemeRegularityMetrics metrics =
                schemeRegularityRepository.getSchemeRegularityMetricsByDepartment(parentDepartmentId, startDate, endDate);

        BigDecimal averageRegularity = BigDecimal.ZERO;
        if (metrics.schemeCount() > 0 && daysInRange > 0) {
            BigDecimal denominator = BigDecimal.valueOf((long) metrics.schemeCount() * daysInRange);
            averageRegularity = BigDecimal.valueOf(metrics.totalSupplyDays())
                    .divide(denominator, 4, RoundingMode.HALF_UP);
        }

        AverageSchemeRegularityResponse response = AverageSchemeRegularityResponse.builder()
                .lgdId(null)
                .parentDepartmentId(parentDepartmentId)
                .startDate(startDate)
                .endDate(endDate)
                .daysInRange(daysInRange)
                .schemeCount(metrics.schemeCount())
                .totalSupplyDays(metrics.totalSupplyDays())
                .averageRegularity(averageRegularity)
                .build();
        writeToCache(cacheKey, response);
        return response;
    }

    @Override
    public ReadingSubmissionRateResponse getReadingSubmissionRateByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate) {
        validateDepartmentInput(parentDepartmentId);
        validateDateRange(startDate, endDate);

        String cacheKey = READING_SUBMISSION_RATE_CACHE_PREFIX
                + ":department:" + parentDepartmentId
                + ":start:" + startDate
                + ":end:" + endDate;
        ReadingSubmissionRateResponse cached = readFromCache(cacheKey, ReadingSubmissionRateResponse.class);
        if (cached != null) {
            return cached;
        }

        int daysInRange = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        SchemeRegularityRepository.SchemeRegularityMetrics metrics =
                schemeRegularityRepository.getReadingSubmissionRateMetricsByDepartment(parentDepartmentId, startDate, endDate);

        BigDecimal readingSubmissionRate = BigDecimal.ZERO;
        if (metrics.schemeCount() > 0 && daysInRange > 0) {
            BigDecimal denominator = BigDecimal.valueOf((long) metrics.schemeCount() * daysInRange);
            readingSubmissionRate = BigDecimal.valueOf(metrics.totalSupplyDays())
                    .divide(denominator, 4, RoundingMode.HALF_UP);
        }

        ReadingSubmissionRateResponse response = ReadingSubmissionRateResponse.builder()
                .lgdId(null)
                .parentDepartmentId(parentDepartmentId)
                .startDate(startDate)
                .endDate(endDate)
                .daysInRange(daysInRange)
                .schemeCount(metrics.schemeCount())
                .totalSubmissionDays(metrics.totalSupplyDays())
                .readingSubmissionRate(readingSubmissionRate)
                .build();
        writeToCache(cacheKey, response);
        return response;
    }

    @Override
    public AverageWaterSupplyResponse getAverageWaterSupplyPerScheme(
            Integer tenantId, LocalDate startDate, LocalDate endDate) {
        validateTenantInput(tenantId);
        validateDateRange(startDate, endDate);

        String cacheKey = ":water_supply:tenant:" + tenantId
                + ":start:" + startDate
                + ":end:" + endDate
                + ":v2";
        AverageWaterSupplyResponse cached = readFromCache(cacheKey, AverageWaterSupplyResponse.class);
        if (cached != null) {
            return cached;
        }

        int daysInRange = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        List<SchemeRegularityRepository.SchemeWaterSupplyMetrics> metrics =
                schemeRegularityRepository.getAverageWaterSupplyPerScheme(tenantId, startDate, endDate);

        List<AverageWaterSupplyResponse.SchemeWaterSupply> schemes = metrics.stream()
                .map(m -> AverageWaterSupplyResponse.SchemeWaterSupply.builder()
                        .schemeId(m.schemeId())
                        .schemeName(m.schemeName())
                        .householdCount(m.householdCount())
                        .totalWaterSuppliedLiters(m.totalWaterSuppliedLiters())
                        .supplyDays(m.supplyDays())
                        .avgLitersPerHousehold(m.averageLitersPerHousehold())
                        .build())
                .toList();

        AverageWaterSupplyResponse response = AverageWaterSupplyResponse.builder()
                .tenantId(tenantId)
                .stateCode(getTenantStateCode(tenantId))
                .startDate(startDate)
                .endDate(endDate)
                .daysInRange(daysInRange)
                .schemeCount(schemes.size())
                .schemes(schemes)
                .childRegionCount(0)
                .childRegions(List.of())
                .build();
        writeToCache(cacheKey, response);
        return response;
    }

    @Override
    public AverageWaterSupplyResponse getAverageWaterSupplyPerNation(
            LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        String cacheKey = ":water_supply:nation"
                + ":start:" + startDate
                + ":end:" + endDate
                + ":v2";
        AverageWaterSupplyResponse cached = readFromCache(cacheKey, AverageWaterSupplyResponse.class);
        if (cached != null) {
            return cached;
        }

        int daysInRange = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        List<SchemeRegularityRepository.ChildRegionWaterSupplyMetrics> metrics =
                schemeRegularityRepository.getAverageWaterSupplyPerNation(startDate, endDate);

        List<AverageWaterSupplyResponse.ChildRegionWaterSupply> childRegions = metrics.stream()
                .map(m -> AverageWaterSupplyResponse.ChildRegionWaterSupply.builder()
                        .lgdId(null)
                        .departmentId(null)
                        .title(m.title())
                        .totalHouseholdCount(m.totalHouseholdCount())
                        .totalWaterSuppliedLiters(m.totalWaterSuppliedLiters())
                        .schemeCount(m.schemeCount())
                        .avgWaterSupplyPerScheme(m.avgWaterSupplyPerScheme())
                        .build())
                .toList();

        AverageWaterSupplyResponse response = AverageWaterSupplyResponse.builder()
                .tenantId(null)
                .stateCode(null)
                .startDate(startDate)
                .endDate(endDate)
                .daysInRange(daysInRange)
                .schemeCount(0)
                .schemes(List.of())
                .childRegionCount(childRegions.size())
                .childRegions(childRegions)
                .build();
        writeToCache(cacheKey, response);
        return response;
    }

    @Override
    public AverageWaterSupplyResponse getAverageWaterSupplyPerSchemeByLgd(
            Integer tenantId, Integer lgdId, LocalDate startDate, LocalDate endDate) {
        validateTenantInput(tenantId);
        validateLgdInput(lgdId);
        validateDateRange(startDate, endDate);
        // #region agent log
        appendDebugLog(
                "H1",
                "SchemeRegularityServiceImpl:getAverageWaterSupplyPerSchemeByLgd:entry",
                "Entered average-per-region LGD branch",
                Map.of(
                        "tenantId", tenantId,
                        "lgdId", lgdId,
                        "startDate", String.valueOf(startDate),
                        "endDate", String.valueOf(endDate)));
        // #endregion

        String cacheKey = ":water_supply:tenant:" + tenantId
                + ":lgd:" + lgdId
                + ":start:" + startDate
                + ":end:" + endDate
                + ":v2";
        AverageWaterSupplyResponse cached = readFromCache(cacheKey, AverageWaterSupplyResponse.class);
        if (cached != null) {
            return cached;
        }

        int daysInRange = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        List<SchemeRegularityRepository.ChildRegionWaterSupplyMetrics> metrics;
        try {
            metrics = schemeRegularityRepository.getAverageWaterSupplyPerSchemeByLgd(tenantId, lgdId, startDate, endDate);
        } catch (Exception ex) {
            // #region agent log
            appendDebugLog(
                    "H2",
                    "SchemeRegularityServiceImpl:getAverageWaterSupplyPerSchemeByLgd:repo_exception",
                    "LGD branch repository call failed",
                    Map.of("errorType", ex.getClass().getName(), "errorMessage", String.valueOf(ex.getMessage())));
            // #endregion
            throw ex;
        }
        // #region agent log
        appendDebugLog(
                "H3",
                "SchemeRegularityServiceImpl:getAverageWaterSupplyPerSchemeByLgd:repo_success",
                "LGD branch repository call succeeded",
                Map.of("daysInRange", daysInRange, "metricRows", metrics.size()));
        // #endregion

        List<AverageWaterSupplyResponse.ChildRegionWaterSupply> childRegions = metrics.stream()
                .map(m -> AverageWaterSupplyResponse.ChildRegionWaterSupply.builder()
                        .lgdId(m.lgdId())
                        .departmentId(null)
                        .title(m.title())
                        .totalHouseholdCount(m.totalHouseholdCount())
                        .totalWaterSuppliedLiters(m.totalWaterSuppliedLiters())
                        .schemeCount(m.schemeCount())
                        .avgWaterSupplyPerScheme(m.avgWaterSupplyPerScheme())
                        .build())
                .toList();

        AverageWaterSupplyResponse response = AverageWaterSupplyResponse.builder()
                .tenantId(tenantId)
                .stateCode(getTenantStateCode(tenantId))
                .startDate(startDate)
                .endDate(endDate)
                .daysInRange(daysInRange)
                .schemeCount(0)
                .schemes(List.of())
                .childRegionCount(childRegions.size())
                .childRegions(childRegions)
                .build();
        writeToCache(cacheKey, response);
        return response;
    }

    @Override
    public AverageWaterSupplyResponse getAverageWaterSupplyPerSchemeByDepartment(
            Integer tenantId, Integer parentDepartmentId, LocalDate startDate, LocalDate endDate) {
        validateTenantInput(tenantId);
        validateDepartmentInput(parentDepartmentId);
        validateDateRange(startDate, endDate);
        // #region agent log
        appendDebugLog(
                "H4",
                "SchemeRegularityServiceImpl:getAverageWaterSupplyPerSchemeByDepartment:entry",
                "Entered average-per-region department branch",
                Map.of(
                        "tenantId", tenantId,
                        "parentDepartmentId", parentDepartmentId,
                        "startDate", String.valueOf(startDate),
                        "endDate", String.valueOf(endDate)));
        // #endregion

        String cacheKey = ":water_supply:tenant:" + tenantId
                + ":department:" + parentDepartmentId
                + ":start:" + startDate
                + ":end:" + endDate
                + ":v2";
        AverageWaterSupplyResponse cached = readFromCache(cacheKey, AverageWaterSupplyResponse.class);
        if (cached != null) {
            return cached;
        }

        int daysInRange = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        List<SchemeRegularityRepository.ChildRegionWaterSupplyMetrics> metrics;
        try {
            metrics = schemeRegularityRepository.getAverageWaterSupplyPerSchemeByDepartment(tenantId, parentDepartmentId, startDate, endDate);
        } catch (Exception ex) {
            // #region agent log
            appendDebugLog(
                    "H5",
                    "SchemeRegularityServiceImpl:getAverageWaterSupplyPerSchemeByDepartment:repo_exception",
                    "Department branch repository call failed",
                    Map.of("errorType", ex.getClass().getName(), "errorMessage", String.valueOf(ex.getMessage())));
            // #endregion
            throw ex;
        }
        // #region agent log
        appendDebugLog(
                "H5",
                "SchemeRegularityServiceImpl:getAverageWaterSupplyPerSchemeByDepartment:repo_success",
                "Department branch repository call succeeded",
                Map.of("daysInRange", daysInRange, "metricRows", metrics.size()));
        // #endregion

        List<AverageWaterSupplyResponse.ChildRegionWaterSupply> childRegions = metrics.stream()
                .map(m -> AverageWaterSupplyResponse.ChildRegionWaterSupply.builder()
                        .lgdId(null)
                        .departmentId(m.departmentId())
                        .title(m.title())
                        .totalHouseholdCount(m.totalHouseholdCount())
                        .totalWaterSuppliedLiters(m.totalWaterSuppliedLiters())
                        .schemeCount(m.schemeCount())
                        .avgWaterSupplyPerScheme(m.avgWaterSupplyPerScheme())
                        .build())
                .toList();

        AverageWaterSupplyResponse response = AverageWaterSupplyResponse.builder()
                .tenantId(tenantId)
                .stateCode(getTenantStateCode(tenantId))
                .startDate(startDate)
                .endDate(endDate)
                .daysInRange(daysInRange)
                .schemeCount(0)
                .schemes(List.of())
                .childRegionCount(childRegions.size())
                .childRegions(childRegions)
                .build();
        writeToCache(cacheKey, response);
        return response;
    }

    private void validateLgdInput(Integer lgdId) {
        if (lgdId == null || lgdId <= 0) {
            throw new IllegalArgumentException("lgd_id must be a positive integer");
        }
    }

    private void validateTenantInput(Integer tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenant_id must be a positive integer");
        }
    }

    private void validateDepartmentInput(Integer parentDepartmentId) {
        if (parentDepartmentId == null || parentDepartmentId <= 0) {
            throw new IllegalArgumentException("parent_department_id must be a positive integer");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("start_date and end_date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("end_date must be on or after start_date");
        }
    }

    private String getTenantStateCode(Integer tenantId) {
        DimTenant tenant = dimTenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found for tenant_id: " + tenantId));
        return tenant.getStateCode();
    }

    private <T> T readFromCache(String cacheKey, Class<T> responseClass) {
        try {
            String payload = redisTemplate.opsForValue().get(cacheKey);
            if (payload == null || payload.isBlank()) {
                return null;
            }
            return objectMapper.readValue(payload, responseClass);
        } catch (Exception e) {
            log.warn("Failed to read scheme regularity cache [{}]: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    private void writeToCache(String cacheKey, Object response) {
        try {
            String payload = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, payload, SCHEME_REGULARITY_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to write scheme regularity cache [{}]: {}", cacheKey, e.getMessage());
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
