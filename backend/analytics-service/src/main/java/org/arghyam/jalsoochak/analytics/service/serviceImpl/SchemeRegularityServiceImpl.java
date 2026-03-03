package org.arghyam.jalsoochak.analytics.service.serviceImpl;

import org.arghyam.jalsoochak.analytics.dto.response.AverageSchemeRegularityResponse;
import org.arghyam.jalsoochak.analytics.dto.response.ReadingSubmissionRateResponse;
import org.arghyam.jalsoochak.analytics.repository.SchemeRegularityRepository;
import org.arghyam.jalsoochak.analytics.service.SchemeRegularityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemeRegularityServiceImpl implements SchemeRegularityService {

    private static final Duration SCHEME_REGULARITY_CACHE_TTL = Duration.ofHours(24);
    private static final String SCHEME_REGULARITY_CACHE_PREFIX = ":scheme_regularity";
    private static final String READING_SUBMISSION_RATE_CACHE_PREFIX = ":reading_submission_rate";

    private final SchemeRegularityRepository schemeRegularityRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public AverageSchemeRegularityResponse getAverageSchemeRegularity(Integer lgdId, LocalDate startDate, LocalDate endDate) {
        validateInput(lgdId, startDate, endDate);

        String cacheKey = SCHEME_REGULARITY_CACHE_PREFIX
                + ":lgd:" + lgdId
                + ":start:" + startDate
                + ":end:" + endDate;
        AverageSchemeRegularityResponse cached = readFromCache(cacheKey, AverageSchemeRegularityResponse.class);
        if (cached != null) {
            return cached;
        }

        int daysInRange = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        SchemeRegularityRepository.SchemeRegularityMetrics metrics =
                schemeRegularityRepository.getSchemeRegularityMetrics(lgdId, startDate, endDate);

        BigDecimal averageRegularity = BigDecimal.ZERO;
        if (metrics.schemeCount() > 0 && daysInRange > 0) {
            BigDecimal denominator = BigDecimal.valueOf((long) metrics.schemeCount() * daysInRange);
            averageRegularity = BigDecimal.valueOf(metrics.totalSupplyDays())
                    .divide(denominator, 4, RoundingMode.HALF_UP);
        }

        AverageSchemeRegularityResponse response = AverageSchemeRegularityResponse.builder()
                .lgdId(lgdId)
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
        validateInput(lgdId, startDate, endDate);

        String cacheKey = READING_SUBMISSION_RATE_CACHE_PREFIX
                + ":lgd:" + lgdId
                + ":start:" + startDate
                + ":end:" + endDate;
        ReadingSubmissionRateResponse cached = readFromCache(cacheKey, ReadingSubmissionRateResponse.class);
        if (cached != null) {
            return cached;
        }

        int daysInRange = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        SchemeRegularityRepository.SchemeRegularityMetrics metrics =
                schemeRegularityRepository.getReadingSubmissionRateMetrics(lgdId, startDate, endDate);

        BigDecimal readingSubmissionRate = BigDecimal.ZERO;
        if (metrics.schemeCount() > 0 && daysInRange > 0) {
            BigDecimal denominator = BigDecimal.valueOf((long) metrics.schemeCount() * daysInRange);
            readingSubmissionRate = BigDecimal.valueOf(metrics.totalSupplyDays())
                    .divide(denominator, 4, RoundingMode.HALF_UP);
        }

        ReadingSubmissionRateResponse response = ReadingSubmissionRateResponse.builder()
                .lgdId(lgdId)
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

    private void validateInput(Integer lgdId, LocalDate startDate, LocalDate endDate) {
        if (lgdId == null || lgdId <= 0) {
            throw new IllegalArgumentException("lgd_id must be a positive integer");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("start_date and end_date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("end_date must be on or after start_date");
        }
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
}
