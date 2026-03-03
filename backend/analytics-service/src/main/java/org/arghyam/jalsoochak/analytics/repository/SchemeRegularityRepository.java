package org.arghyam.jalsoochak.analytics.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SchemeRegularityRepository {

    private final JdbcTemplate jdbcTemplate;

    public SchemeRegularityMetrics getSchemeRegularityMetrics(Integer lgdId, LocalDate startDate, LocalDate endDate) {
        Integer lgdLevel = getLgdLevel(lgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("lgd_id not found in dim_lgd_location_table: " + lgdId);
        }
        String schemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);

        String sql = String.format("""
                WITH schemes_in_lgd AS (
                    SELECT DISTINCT s.scheme_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%1$s = ?
                ),
                scheme_supply_days AS (
                    SELECT m.scheme_id, COUNT(DISTINCT m.reading_date)::int AS supply_days
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_lgd sl
                        ON sl.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading > 0
                    GROUP BY m.scheme_id
                )
                SELECT
                    (SELECT COUNT(*)::int FROM schemes_in_lgd) AS scheme_count,
                    COALESCE((SELECT SUM(supply_days)::int FROM scheme_supply_days), 0) AS total_supply_days
                """, schemeLgdColumn);

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, lgdId, startDate, endDate);
        int schemeCount = result.get("scheme_count") instanceof Number value ? value.intValue() : 0;
        int totalSupplyDays = result.get("total_supply_days") instanceof Number value ? value.intValue() : 0;

        return new SchemeRegularityMetrics(schemeCount, totalSupplyDays);
    }

    public SchemeRegularityMetrics getReadingSubmissionRateMetrics(Integer lgdId, LocalDate startDate, LocalDate endDate) {
        Integer lgdLevel = getLgdLevel(lgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("lgd_id not found in dim_lgd_location_table: " + lgdId);
        }
        String schemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);

        String sql = String.format("""
                WITH schemes_in_lgd AS (
                    SELECT DISTINCT s.scheme_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%1$s = ?
                ),
                scheme_submission_days AS (
                    SELECT m.scheme_id, COUNT(DISTINCT m.reading_date)::int AS submission_days
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_lgd sl
                        ON sl.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading >= 0
                    GROUP BY m.scheme_id
                )
                SELECT
                    (SELECT COUNT(*)::int FROM schemes_in_lgd) AS scheme_count,
                    COALESCE((SELECT SUM(submission_days)::int FROM scheme_submission_days), 0) AS total_supply_days
                """, schemeLgdColumn);

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, lgdId, startDate, endDate);
        int schemeCount = result.get("scheme_count") instanceof Number value ? value.intValue() : 0;
        int totalSupplyDays = result.get("total_supply_days") instanceof Number value ? value.intValue() : 0;

        return new SchemeRegularityMetrics(schemeCount, totalSupplyDays);
    }

    private Integer getLgdLevel(Integer lgdId) {
        String sql = """
                SELECT l.lgd_level
                FROM analytics_schema.dim_lgd_location_table l
                WHERE l.lgd_id = ?
                LIMIT 1
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> (Integer) rs.getObject("lgd_level"), lgdId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private String resolveSchemeLgdColumn(Integer lgdLevel) {
        return switch (lgdLevel) {
            case 0 -> "parent_lgd_location_id";
            case 1 -> "level_1_lgd_id";
            case 2 -> "level_2_lgd_id";
            case 3 -> "level_3_lgd_id";
            case 4 -> "level_4_lgd_id";
            case 5 -> "level_5_lgd_id";
            case 6 -> "level_6_lgd_id";
            default -> throw new IllegalArgumentException("Unsupported lgd_level: " + lgdLevel);
        };
    }

    public record SchemeRegularityMetrics(int schemeCount, int totalSupplyDays) {
    }
}
