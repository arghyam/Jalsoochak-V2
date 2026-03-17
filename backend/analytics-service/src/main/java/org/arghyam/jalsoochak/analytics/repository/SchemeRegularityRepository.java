package org.arghyam.jalsoochak.analytics.repository;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.analytics.enums.PeriodScale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SchemeRegularityRepository {

    private final JdbcTemplate jdbcTemplate;

    public SchemeRegularityMetrics getSchemeRegularityMetrics(Integer parentLgdId, LocalDate startDate, LocalDate endDate) {
        Integer lgdLevel = getLgdLevel(parentLgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("parent_lgd_id not found in dim_lgd_location_table: " + parentLgdId);
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

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, parentLgdId, startDate, endDate);
        int schemeCount = result.get("scheme_count") instanceof Number value ? value.intValue() : 0;
        int totalSupplyDays = result.get("total_supply_days") instanceof Number value ? value.intValue() : 0;

        return new SchemeRegularityMetrics(schemeCount, totalSupplyDays);
    }

    public SchemeRegularityMetrics getReadingSubmissionRateMetricsByLgd(Integer parentLgdId, LocalDate startDate, LocalDate endDate) {
        Integer lgdLevel = getLgdLevel(parentLgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("lgd_id not found in dim_lgd_location_table: " + parentLgdId);
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

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, parentLgdId, startDate, endDate);
        int schemeCount = result.get("scheme_count") instanceof Number value ? value.intValue() : 0;
        int totalSupplyDays = result.get("total_supply_days") instanceof Number value ? value.intValue() : 0;

        return new SchemeRegularityMetrics(schemeCount, totalSupplyDays);
    }

    public SchemeRegularityMetrics getSchemeRegularityMetricsByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate) {
        Integer departmentLevel = getDepartmentLevel(parentDepartmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException(
                    "parent_department_id not found in dim_department_location_table: " + parentDepartmentId);
        }
        String schemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);

        String sql = String.format("""
                WITH schemes_in_department AS (
                    SELECT DISTINCT s.scheme_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%1$s = ?
                ),
                scheme_supply_days AS (
                    SELECT m.scheme_id, COUNT(DISTINCT m.reading_date)::int AS supply_days
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_department sd
                        ON sd.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading > 0
                    GROUP BY m.scheme_id
                )
                SELECT
                    (SELECT COUNT(*)::int FROM schemes_in_department) AS scheme_count,
                    COALESCE((SELECT SUM(supply_days)::int FROM scheme_supply_days), 0) AS total_supply_days
                """, schemeDepartmentColumn);

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, parentDepartmentId, startDate, endDate);
        int schemeCount = result.get("scheme_count") instanceof Number value ? value.intValue() : 0;
        int totalSupplyDays = result.get("total_supply_days") instanceof Number value ? value.intValue() : 0;

        return new SchemeRegularityMetrics(schemeCount, totalSupplyDays);
    }

    public SchemeRegularityMetrics getReadingSubmissionRateMetricsByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate) {
        Integer departmentLevel = getDepartmentLevel(parentDepartmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException(
                    "parent_department_id not found in dim_department_location_table: " + parentDepartmentId);
        }
        String schemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);

        String sql = String.format("""
                WITH schemes_in_department AS (
                    SELECT DISTINCT s.scheme_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%1$s = ?
                ),
                scheme_submission_days AS (
                    SELECT m.scheme_id, COUNT(DISTINCT m.reading_date)::int AS submission_days
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_department sd
                        ON sd.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading >= 0
                    GROUP BY m.scheme_id
                )
                SELECT
                    (SELECT COUNT(*)::int FROM schemes_in_department) AS scheme_count,
                    COALESCE((SELECT SUM(submission_days)::int FROM scheme_submission_days), 0) AS total_supply_days
                """, schemeDepartmentColumn);

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, parentDepartmentId, startDate, endDate);
        int schemeCount = result.get("scheme_count") instanceof Number value ? value.intValue() : 0;
        int totalSupplyDays = result.get("total_supply_days") instanceof Number value ? value.intValue() : 0;

        return new SchemeRegularityMetrics(schemeCount, totalSupplyDays);
    }

    public List<ChildRegionReadingSubmissionMetrics> getChildReadingSubmissionRateMetricsByLgd(
            Integer parentLgdId, LocalDate startDate, LocalDate endDate) {
        Integer lgdLevel = getLgdLevel(parentLgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("lgd_id not found in dim_lgd_location_table: " + parentLgdId);
        }
        if (lgdLevel >= 6) {
            throw new IllegalArgumentException("No child LGD level available for parent_lgd_id: " + parentLgdId);
        }
        int childLevel = lgdLevel + 1;
        long daysInRange = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysInRange <= 0) {
            return List.of();
        }

        String parentSchemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);
        String childSchemeLgdColumn = resolveSchemeLgdColumn(childLevel);
        String childRegionParentLgdColumn = resolveChildRegionLgdParentColumn(lgdLevel);

        String sql = String.format("""
                WITH child_regions AS (
                    SELECT
                        l.lgd_id AS child_lgd_id,
                        l.title
                    FROM analytics_schema.dim_lgd_location_table l
                    WHERE l.lgd_level = ?
                      AND l.%1$s = ?
                ),
                schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.%2$s AS child_lgd_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%3$s = ?
                ),
                scheme_submission_days AS (
                    SELECT m.scheme_id, COUNT(DISTINCT m.reading_date)::int AS submission_days
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_scope ss
                        ON ss.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading >= 0
                    GROUP BY m.scheme_id
                )
                SELECT
                    c.child_lgd_id AS lgd_id,
                    c.title,
                    COALESCE(COUNT(s.scheme_id), 0)::int AS scheme_count,
                    COALESCE(SUM(sd.submission_days), 0)::int AS total_submission_days
                FROM child_regions c
                LEFT JOIN schemes_in_scope s
                    ON s.child_lgd_id = c.child_lgd_id
                LEFT JOIN scheme_submission_days sd
                    ON sd.scheme_id = s.scheme_id
                GROUP BY c.child_lgd_id, c.title
                ORDER BY c.child_lgd_id
                """, childRegionParentLgdColumn, childSchemeLgdColumn, parentSchemeLgdColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    int schemeCount = rs.getInt("scheme_count");
                    int totalSubmissionDays = rs.getInt("total_submission_days");
                    BigDecimal readingSubmissionRate = BigDecimal.ZERO;
                    if (schemeCount > 0) {
                        readingSubmissionRate = BigDecimal.valueOf(totalSubmissionDays)
                                .divide(BigDecimal.valueOf((long) schemeCount * daysInRange), 4, RoundingMode.HALF_UP);
                    }
                    return new ChildRegionReadingSubmissionMetrics(
                            rs.getInt("lgd_id"),
                            null,
                            rs.getString("title"),
                            schemeCount,
                            totalSubmissionDays,
                            readingSubmissionRate);
                },
                childLevel,
                parentLgdId,
                parentLgdId,
                startDate,
                endDate);
    }

    public List<ChildRegionReadingSubmissionMetrics> getChildReadingSubmissionRateMetricsByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate) {
        Integer departmentLevel = getDepartmentLevel(parentDepartmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException(
                    "parent_department_id not found in dim_department_location_table: " + parentDepartmentId);
        }
        if (departmentLevel >= 6) {
            throw new IllegalArgumentException("No child department level available for parent_department_id: " + parentDepartmentId);
        }
        int childLevel = departmentLevel + 1;
        long daysInRange = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysInRange <= 0) {
            return List.of();
        }

        String parentSchemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);
        String childSchemeDepartmentColumn = resolveSchemeDepartmentColumn(childLevel);
        String childRegionParentDepartmentColumn = resolveChildRegionDepartmentParentColumn(departmentLevel);

        String sql = String.format("""
                WITH child_regions AS (
                    SELECT
                        d.department_id AS child_department_id,
                        d.title
                    FROM analytics_schema.dim_department_location_table d
                    WHERE d.department_level = ?
                      AND d.%1$s = ?
                ),
                schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.%2$s AS child_department_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%3$s = ?
                ),
                scheme_submission_days AS (
                    SELECT m.scheme_id, COUNT(DISTINCT m.reading_date)::int AS submission_days
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_scope ss
                        ON ss.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading >= 0
                    GROUP BY m.scheme_id
                )
                SELECT
                    c.child_department_id AS department_id,
                    c.title,
                    COALESCE(COUNT(s.scheme_id), 0)::int AS scheme_count,
                    COALESCE(SUM(sd.submission_days), 0)::int AS total_submission_days
                FROM child_regions c
                LEFT JOIN schemes_in_scope s
                    ON s.child_department_id = c.child_department_id
                LEFT JOIN scheme_submission_days sd
                    ON sd.scheme_id = s.scheme_id
                GROUP BY c.child_department_id, c.title
                ORDER BY c.child_department_id
                """, childRegionParentDepartmentColumn, childSchemeDepartmentColumn, parentSchemeDepartmentColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    int schemeCount = rs.getInt("scheme_count");
                    int totalSubmissionDays = rs.getInt("total_submission_days");
                    BigDecimal readingSubmissionRate = BigDecimal.ZERO;
                    if (schemeCount > 0) {
                        readingSubmissionRate = BigDecimal.valueOf(totalSubmissionDays)
                                .divide(BigDecimal.valueOf((long) schemeCount * daysInRange), 4, RoundingMode.HALF_UP);
                    }
                    return new ChildRegionReadingSubmissionMetrics(
                            null,
                            rs.getInt("department_id"),
                            rs.getString("title"),
                            schemeCount,
                            totalSubmissionDays,
                            readingSubmissionRate);
                },
                childLevel,
                parentDepartmentId,
                parentDepartmentId,
                startDate,
                endDate);
    }

    public List<ChildRegionSchemeRegularityMetrics> getChildSchemeRegularityMetricsByLgd(
            Integer parentLgdId, LocalDate startDate, LocalDate endDate) {
        Integer lgdLevel = getLgdLevel(parentLgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("parent_lgd_id not found in dim_lgd_location_table: " + parentLgdId);
        }
        if (lgdLevel >= 6) {
            throw new IllegalArgumentException("No child LGD level available for parent_lgd_id: " + parentLgdId);
        }
        int childLevel = lgdLevel + 1;
        long daysInRange = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysInRange <= 0) {
            return List.of();
        }

        String parentSchemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);
        String childSchemeLgdColumn = resolveSchemeLgdColumn(childLevel);
        String childRegionParentLgdColumn = resolveChildRegionLgdParentColumn(lgdLevel);

        String sql = String.format("""
                WITH child_regions AS (
                    SELECT
                        l.lgd_id AS child_lgd_id,
                        l.title
                    FROM analytics_schema.dim_lgd_location_table l
                    WHERE l.lgd_level = ?
                      AND l.%1$s = ?
                ),
                schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.%2$s AS child_lgd_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%3$s = ?
                ),
                scheme_supply_days AS (
                    SELECT m.scheme_id, COUNT(DISTINCT m.reading_date)::int AS supply_days
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_scope ss
                        ON ss.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading > 0
                    GROUP BY m.scheme_id
                )
                SELECT
                    c.child_lgd_id AS lgd_id,
                    c.title,
                    COALESCE(COUNT(s.scheme_id), 0)::int AS scheme_count,
                    COALESCE(SUM(sd.supply_days), 0)::int AS total_supply_days
                FROM child_regions c
                LEFT JOIN schemes_in_scope s
                    ON s.child_lgd_id = c.child_lgd_id
                LEFT JOIN scheme_supply_days sd
                    ON sd.scheme_id = s.scheme_id
                GROUP BY c.child_lgd_id, c.title
                ORDER BY c.child_lgd_id
                """, childRegionParentLgdColumn, childSchemeLgdColumn, parentSchemeLgdColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    int schemeCount = rs.getInt("scheme_count");
                    int totalSupplyDays = rs.getInt("total_supply_days");
                    BigDecimal averageRegularity = BigDecimal.ZERO;
                    if (schemeCount > 0) {
                        averageRegularity = BigDecimal.valueOf(totalSupplyDays)
                                .divide(BigDecimal.valueOf((long) schemeCount * daysInRange), 4, RoundingMode.HALF_UP);
                    }
                    return new ChildRegionSchemeRegularityMetrics(
                            rs.getInt("lgd_id"),
                            null,
                            rs.getString("title"),
                            schemeCount,
                            totalSupplyDays,
                            averageRegularity);
                },
                childLevel,
                parentLgdId,
                parentLgdId,
                startDate,
                endDate);
    }

    public List<ChildRegionSchemeRegularityMetrics> getChildSchemeRegularityMetricsByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate) {
        Integer departmentLevel = getDepartmentLevel(parentDepartmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException(
                    "parent_department_id not found in dim_department_location_table: " + parentDepartmentId);
        }
        if (departmentLevel >= 6) {
            throw new IllegalArgumentException("No child department level available for parent_department_id: " + parentDepartmentId);
        }
        int childLevel = departmentLevel + 1;
        long daysInRange = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysInRange <= 0) {
            return List.of();
        }

        String parentSchemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);
        String childSchemeDepartmentColumn = resolveSchemeDepartmentColumn(childLevel);
        String childRegionParentDepartmentColumn = resolveChildRegionDepartmentParentColumn(departmentLevel);

        String sql = String.format("""
                WITH child_regions AS (
                    SELECT
                        d.department_id AS child_department_id,
                        d.title
                    FROM analytics_schema.dim_department_location_table d
                    WHERE d.department_level = ?
                      AND d.%1$s = ?
                ),
                schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.%2$s AS child_department_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%3$s = ?
                ),
                scheme_supply_days AS (
                    SELECT m.scheme_id, COUNT(DISTINCT m.reading_date)::int AS supply_days
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_scope ss
                        ON ss.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading > 0
                    GROUP BY m.scheme_id
                )
                SELECT
                    c.child_department_id AS department_id,
                    c.title,
                    COALESCE(COUNT(s.scheme_id), 0)::int AS scheme_count,
                    COALESCE(SUM(sd.supply_days), 0)::int AS total_supply_days
                FROM child_regions c
                LEFT JOIN schemes_in_scope s
                    ON s.child_department_id = c.child_department_id
                LEFT JOIN scheme_supply_days sd
                    ON sd.scheme_id = s.scheme_id
                GROUP BY c.child_department_id, c.title
                ORDER BY c.child_department_id
                """, childRegionParentDepartmentColumn, childSchemeDepartmentColumn, parentSchemeDepartmentColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    int schemeCount = rs.getInt("scheme_count");
                    int totalSupplyDays = rs.getInt("total_supply_days");
                    BigDecimal averageRegularity = BigDecimal.ZERO;
                    if (schemeCount > 0) {
                        averageRegularity = BigDecimal.valueOf(totalSupplyDays)
                                .divide(BigDecimal.valueOf((long) schemeCount * daysInRange), 4, RoundingMode.HALF_UP);
                    }
                    return new ChildRegionSchemeRegularityMetrics(
                            null,
                            rs.getInt("department_id"),
                            rs.getString("title"),
                            schemeCount,
                            totalSupplyDays,
                            averageRegularity);
                },
                childLevel,
                parentDepartmentId,
                parentDepartmentId,
                startDate,
                endDate);
    }

    public List<OutageReasonSchemeCount> getOutageReasonSchemeCountByLgd(
            Integer lgdId, LocalDate startDate, LocalDate endDate) {
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
                )
                SELECT
                    f.outage_reason,
                    COUNT(DISTINCT f.scheme_id)::int AS scheme_count
                FROM analytics_schema.fact_water_quantity_table f
                JOIN schemes_in_lgd sl
                    ON sl.scheme_id = f.scheme_id
                WHERE f.outage_reason IS NOT NULL
                  AND f.date BETWEEN ? AND ?
                GROUP BY f.outage_reason
                ORDER BY f.outage_reason
                """, schemeLgdColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new OutageReasonSchemeCount(
                        (Integer) rs.getObject("outage_reason"),
                        rs.getInt("scheme_count")),
                lgdId,
                startDate,
                endDate);
    }

    public List<OutageReasonSchemeCount> getOutageReasonSchemeCountByDepartment(
            Integer departmentId, LocalDate startDate, LocalDate endDate) {
        Integer departmentLevel = getDepartmentLevel(departmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException("department_id not found in dim_department_location_table: " + departmentId);
        }
        String schemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);

        String sql = String.format("""
                WITH schemes_in_department AS (
                    SELECT DISTINCT s.scheme_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%1$s = ?
                )
                SELECT
                    f.outage_reason,
                    COUNT(DISTINCT f.scheme_id)::int AS scheme_count
                FROM analytics_schema.fact_water_quantity_table f
                JOIN schemes_in_department sd
                    ON sd.scheme_id = f.scheme_id
                WHERE f.outage_reason IS NOT NULL
                  AND f.date BETWEEN ? AND ?
                GROUP BY f.outage_reason
                ORDER BY f.outage_reason
                """, schemeDepartmentColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new OutageReasonSchemeCount(
                        (Integer) rs.getObject("outage_reason"),
                        rs.getInt("scheme_count")),
                departmentId,
                startDate,
                endDate);
    }

    public List<OutageReasonSchemeCount> getOutageReasonSchemeCountByUser(
            Integer userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                WITH user_schemes AS (
                    SELECT DISTINCT usm.scheme_id
                    FROM analytics_schema.dim_user_scheme_mapping_table usm
                    WHERE usm.user_id = ?
                )
                SELECT
                    f.outage_reason,
                    COUNT(DISTINCT f.scheme_id)::int AS scheme_count
                FROM analytics_schema.fact_water_quantity_table f
                JOIN user_schemes us
                    ON us.scheme_id = f.scheme_id
                WHERE f.outage_reason IS NOT NULL
                  AND f.date BETWEEN ? AND ?
                GROUP BY f.outage_reason
                ORDER BY f.outage_reason
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new OutageReasonSchemeCount(
                        (Integer) rs.getObject("outage_reason"),
                        rs.getInt("scheme_count")),
                userId,
                startDate,
                endDate);
    }

    public List<DailyOutageReasonSchemeCount> getDailyOutageReasonSchemeCountByUser(
            Integer userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                WITH user_schemes AS (
                    SELECT DISTINCT usm.scheme_id
                    FROM analytics_schema.dim_user_scheme_mapping_table usm
                    WHERE usm.user_id = ?
                )
                SELECT
                    f.date,
                    f.outage_reason,
                    COUNT(DISTINCT f.scheme_id)::int AS scheme_count
                FROM analytics_schema.fact_water_quantity_table f
                JOIN user_schemes us
                    ON us.scheme_id = f.scheme_id
                WHERE f.outage_reason IS NOT NULL
                  AND f.date BETWEEN ? AND ?
                GROUP BY f.date, f.outage_reason
                ORDER BY f.date, f.outage_reason
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DailyOutageReasonSchemeCount(
                        rs.getObject("date", LocalDate.class),
                        (Integer) rs.getObject("outage_reason"),
                        rs.getInt("scheme_count")),
                userId,
                startDate,
                endDate);
    }

    public Integer getSchemeCountByUser(Integer userId) {
        String sql = """
                SELECT COALESCE(COUNT(DISTINCT usm.scheme_id), 0)::int AS scheme_count
                FROM analytics_schema.dim_user_scheme_mapping_table usm
                WHERE usm.user_id = ?
                """;

        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }

    public SubmissionStatusCount getSubmissionStatusCountByUser(
            Integer userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT
                    COALESCE(
                        COUNT(*) FILTER (
                            WHERE m.extracted_reading IS NOT NULL
                              AND m.extracted_reading = m.confirmed_reading
                        ),
                        0
                    )::int AS compliant_submission_count,
                    COALESCE(
                        COUNT(*) FILTER (
                            WHERE m.extracted_reading IS NOT NULL
                              AND m.extracted_reading IS DISTINCT FROM m.confirmed_reading
                        ),
                        0
                    )::int AS anomalous_submission_count
                FROM analytics_schema.fact_meter_reading_table m
                WHERE m.user_id = ?
                  AND m.reading_date BETWEEN ? AND ?
                """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, userId, startDate, endDate);
        int compliantSubmissionCount =
                result.get("compliant_submission_count") instanceof Number value ? value.intValue() : 0;
        int anomalousSubmissionCount =
                result.get("anomalous_submission_count") instanceof Number value ? value.intValue() : 0;
        return new SubmissionStatusCount(compliantSubmissionCount, anomalousSubmissionCount);
    }

    public List<DailySubmissionSchemeCount> getDailySubmissionSchemeCountByUser(
            Integer userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                WITH user_schemes AS (
                    SELECT DISTINCT usm.scheme_id
                    FROM analytics_schema.dim_user_scheme_mapping_table usm
                    WHERE usm.user_id = ?
                )
                SELECT
                    m.reading_date AS date,
                    COUNT(DISTINCT m.scheme_id)::int AS submitted_scheme_count
                FROM analytics_schema.fact_meter_reading_table m
                JOIN user_schemes us
                    ON us.scheme_id = m.scheme_id
                WHERE m.user_id = ?
                  AND m.extracted_reading IS NOT NULL
                  AND m.reading_date BETWEEN ? AND ?
                GROUP BY m.reading_date
                ORDER BY m.reading_date
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DailySubmissionSchemeCount(
                        rs.getObject("date", LocalDate.class),
                        rs.getInt("submitted_scheme_count")),
                userId,
                userId,
                startDate,
                endDate);
    }

    public List<ChildRegionRef> getChildRegionsByLgd(Integer lgdId) {
        Integer lgdLevel = getLgdLevel(lgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("lgd_id not found in dim_lgd_location_table: " + lgdId);
        }
        if (lgdLevel >= 6) {
            return List.of();
        }

        int childLevel = lgdLevel + 1;
        String childRegionParentLgdColumn = resolveChildRegionLgdParentColumn(lgdLevel);

        String sql = String.format("""
                SELECT
                    l.lgd_id,
                    l.title
                FROM analytics_schema.dim_lgd_location_table l
                WHERE l.lgd_level = ?
                  AND l.%1$s = ?
                ORDER BY l.lgd_id
                """, childRegionParentLgdColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ChildRegionRef(rs.getInt("lgd_id"), null, rs.getString("title")),
                childLevel,
                lgdId);
    }

    public List<ChildRegionRef> getChildRegionsByDepartment(Integer departmentId) {
        Integer departmentLevel = getDepartmentLevel(departmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException("department_id not found in dim_department_location_table: " + departmentId);
        }
        if (departmentLevel >= 6) {
            return List.of();
        }

        int childLevel = departmentLevel + 1;
        String childRegionParentDepartmentColumn = resolveChildRegionDepartmentParentColumn(departmentLevel);

        String sql = String.format("""
                SELECT
                    d.department_id,
                    d.title
                FROM analytics_schema.dim_department_location_table d
                WHERE d.department_level = ?
                  AND d.%1$s = ?
                ORDER BY d.department_id
                """, childRegionParentDepartmentColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ChildRegionRef(null, rs.getInt("department_id"), rs.getString("title")),
                childLevel,
                departmentId);
    }

    public List<ChildRegionOutageReasonSchemeCount> getChildOutageReasonSchemeCountByLgd(
            Integer lgdId, LocalDate startDate, LocalDate endDate) {
        Integer lgdLevel = getLgdLevel(lgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("lgd_id not found in dim_lgd_location_table: " + lgdId);
        }
        if (lgdLevel >= 6) {
            return List.of();
        }

        String parentSchemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);
        String childSchemeLgdColumn = resolveSchemeLgdColumn(lgdLevel + 1);

        String sql = String.format("""
                WITH schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.%1$s AS child_lgd_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%2$s = ?
                )
                SELECT
                    ss.child_lgd_id AS lgd_id,
                    f.outage_reason,
                    COUNT(DISTINCT f.scheme_id)::int AS scheme_count
                FROM schemes_in_scope ss
                JOIN analytics_schema.fact_water_quantity_table f
                    ON f.scheme_id = ss.scheme_id
                WHERE f.outage_reason IS NOT NULL
                  AND f.date BETWEEN ? AND ?
                GROUP BY ss.child_lgd_id, f.outage_reason
                ORDER BY ss.child_lgd_id, f.outage_reason
                """, childSchemeLgdColumn, parentSchemeLgdColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ChildRegionOutageReasonSchemeCount(
                        rs.getInt("lgd_id"),
                        null,
                        (Integer) rs.getObject("outage_reason"),
                        rs.getInt("scheme_count")),
                lgdId,
                startDate,
                endDate);
    }

    public List<ChildRegionOutageReasonSchemeCount> getChildOutageReasonSchemeCountByDepartment(
            Integer departmentId, LocalDate startDate, LocalDate endDate) {
        Integer departmentLevel = getDepartmentLevel(departmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException("department_id not found in dim_department_location_table: " + departmentId);
        }
        if (departmentLevel >= 6) {
            return List.of();
        }

        String parentSchemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);
        String childSchemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel + 1);

        String sql = String.format("""
                WITH schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.%1$s AS child_department_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%2$s = ?
                )
                SELECT
                    ss.child_department_id AS department_id,
                    f.outage_reason,
                    COUNT(DISTINCT f.scheme_id)::int AS scheme_count
                FROM schemes_in_scope ss
                JOIN analytics_schema.fact_water_quantity_table f
                    ON f.scheme_id = ss.scheme_id
                WHERE f.outage_reason IS NOT NULL
                  AND f.date BETWEEN ? AND ?
                GROUP BY ss.child_department_id, f.outage_reason
                ORDER BY ss.child_department_id, f.outage_reason
                """, childSchemeDepartmentColumn, parentSchemeDepartmentColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ChildRegionOutageReasonSchemeCount(
                        null,
                        rs.getInt("department_id"),
                        (Integer) rs.getObject("outage_reason"),
                        rs.getInt("scheme_count")),
                departmentId,
                startDate,
                endDate);
    }

    public SchemeStatusCount getSchemeStatusCountByLgd(Integer lgdId) {
        Integer lgdLevel = getLgdLevel(lgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("lgd_id not found in dim_lgd_location_table: " + lgdId);
        }
        String schemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);

        String sql = String.format("""
                SELECT
                    COUNT(*) FILTER (WHERE s.status = 1)::int AS active_scheme_count,
                    COUNT(*) FILTER (WHERE s.status = 0)::int AS inactive_scheme_count
                FROM analytics_schema.dim_scheme_table s
                WHERE s.%1$s = ?
                """, schemeLgdColumn);

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, lgdId);
        int activeSchemeCount = result.get("active_scheme_count") instanceof Number value ? value.intValue() : 0;
        int inactiveSchemeCount = result.get("inactive_scheme_count") instanceof Number value ? value.intValue() : 0;

        return new SchemeStatusCount(activeSchemeCount, inactiveSchemeCount);
    }

    public SchemeStatusCount getSchemeStatusCountByDepartment(Integer departmentId) {
        Integer departmentLevel = getDepartmentLevel(departmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException("department_id not found in dim_department_location_table: " + departmentId);
        }
        String schemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);

        String sql = String.format("""
                SELECT
                    COUNT(*) FILTER (WHERE s.status = 1)::int AS active_scheme_count,
                    COUNT(*) FILTER (WHERE s.status = 0)::int AS inactive_scheme_count
                FROM analytics_schema.dim_scheme_table s
                WHERE s.%1$s = ?
                """, schemeDepartmentColumn);

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, departmentId);
        int activeSchemeCount = result.get("active_scheme_count") instanceof Number value ? value.intValue() : 0;
        int inactiveSchemeCount = result.get("inactive_scheme_count") instanceof Number value ? value.intValue() : 0;

        return new SchemeStatusCount(activeSchemeCount, inactiveSchemeCount);
    }

    public List<SchemeSubmissionMetrics> getTopSchemeSubmissionMetricsByLgd(
            Integer parentLgdId, LocalDate startDate, LocalDate endDate, Integer topSchemeCount) {
        Integer lgdLevel = getLgdLevel(parentLgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("parent_lgd_id not found in dim_lgd_location_table: " + parentLgdId);
        }
        String schemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);

        String sql = String.format("""
                WITH schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.scheme_name,
                        s.status,
                        CASE
                            WHEN s.level_6_lgd_id IS NOT NULL THEN s.level_5_lgd_id
                            WHEN s.level_5_lgd_id IS NOT NULL THEN s.level_4_lgd_id
                            WHEN s.level_4_lgd_id IS NOT NULL THEN s.level_3_lgd_id
                            WHEN s.level_3_lgd_id IS NOT NULL THEN s.level_2_lgd_id
                            WHEN s.level_2_lgd_id IS NOT NULL THEN s.level_1_lgd_id
                            WHEN s.level_1_lgd_id IS NOT NULL THEN s.parent_lgd_location_id
                            ELSE NULL
                        END AS immediate_parent_lgd_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%1$s = ?
                ),
                scheme_submission_days AS (
                    SELECT
                        m.scheme_id,
                        COUNT(DISTINCT m.reading_date)::int AS submission_days,
                        COALESCE(SUM(CASE WHEN m.confirmed_reading > 0 THEN m.confirmed_reading ELSE 0 END), 0)::bigint
                            AS total_water_supplied
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_scope ss
                        ON ss.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading >= 0
                    GROUP BY m.scheme_id
                )
                SELECT
                    ss.scheme_id,
                    ss.scheme_name,
                    ss.status,
                    COALESCE(sd.submission_days, 0)::int AS submission_days,
                    COALESCE(sd.total_water_supplied, 0)::bigint AS total_water_supplied,
                    ss.immediate_parent_lgd_id,
                    pl.lgd_c_name AS immediate_parent_lgd_c_name,
                    pl.title AS immediate_parent_lgd_title,
                    NULL::int AS immediate_parent_department_id,
                    NULL::varchar AS immediate_parent_department_c_name,
                    NULL::varchar AS immediate_parent_department_title
                FROM schemes_in_scope ss
                LEFT JOIN scheme_submission_days sd
                    ON sd.scheme_id = ss.scheme_id
                LEFT JOIN analytics_schema.dim_lgd_location_table pl
                    ON pl.lgd_id = ss.immediate_parent_lgd_id
                ORDER BY
                    (COALESCE(sd.submission_days, 0)::numeric / ?) DESC,
                    ss.scheme_id ASC
                LIMIT ?
                """, schemeLgdColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SchemeSubmissionMetrics(
                        rs.getInt("scheme_id"),
                        rs.getString("scheme_name"),
                        (Integer) rs.getObject("status"),
                        rs.getInt("submission_days"),
                        rs.getLong("total_water_supplied"),
                        (Integer) rs.getObject("immediate_parent_lgd_id"),
                        rs.getString("immediate_parent_lgd_c_name"),
                        rs.getString("immediate_parent_lgd_title"),
                        (Integer) rs.getObject("immediate_parent_department_id"),
                        rs.getString("immediate_parent_department_c_name"),
                        rs.getString("immediate_parent_department_title")),
                parentLgdId,
                startDate,
                endDate,
                ChronoUnit.DAYS.between(startDate, endDate) + 1,
                topSchemeCount);
    }

    public List<SchemeSubmissionMetrics> getTopSchemeSubmissionMetricsByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate, Integer topSchemeCount) {
        Integer departmentLevel = getDepartmentLevel(parentDepartmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException(
                    "parent_department_id not found in dim_department_location_table: " + parentDepartmentId);
        }
        String schemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);

        String sql = String.format("""
                WITH schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.scheme_name,
                        s.status,
                        CASE
                            WHEN s.level_6_dept_id IS NOT NULL THEN s.level_5_dept_id
                            WHEN s.level_5_dept_id IS NOT NULL THEN s.level_4_dept_id
                            WHEN s.level_4_dept_id IS NOT NULL THEN s.level_3_dept_id
                            WHEN s.level_3_dept_id IS NOT NULL THEN s.level_2_dept_id
                            WHEN s.level_2_dept_id IS NOT NULL THEN s.level_1_dept_id
                            WHEN s.level_1_dept_id IS NOT NULL THEN s.parent_department_location_id
                            ELSE NULL
                        END AS immediate_parent_department_id
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%1$s = ?
                ),
                scheme_submission_days AS (
                    SELECT
                        m.scheme_id,
                        COUNT(DISTINCT m.reading_date)::int AS submission_days,
                        COALESCE(SUM(CASE WHEN m.confirmed_reading > 0 THEN m.confirmed_reading ELSE 0 END), 0)::bigint
                            AS total_water_supplied
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_scope ss
                        ON ss.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading >= 0
                    GROUP BY m.scheme_id
                )
                SELECT
                    ss.scheme_id,
                    ss.scheme_name,
                    ss.status,
                    COALESCE(sd.submission_days, 0)::int AS submission_days,
                    COALESCE(sd.total_water_supplied, 0)::bigint AS total_water_supplied,
                    NULL::int AS immediate_parent_lgd_id,
                    NULL::varchar AS immediate_parent_lgd_c_name,
                    NULL::varchar AS immediate_parent_lgd_title,
                    ss.immediate_parent_department_id,
                    pd.department_c_name AS immediate_parent_department_c_name,
                    pd.title AS immediate_parent_department_title
                FROM schemes_in_scope ss
                LEFT JOIN scheme_submission_days sd
                    ON sd.scheme_id = ss.scheme_id
                LEFT JOIN analytics_schema.dim_department_location_table pd
                    ON pd.department_id = ss.immediate_parent_department_id
                ORDER BY
                    (COALESCE(sd.submission_days, 0)::numeric / ?) DESC,
                    ss.scheme_id ASC
                LIMIT ?
                """, schemeDepartmentColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SchemeSubmissionMetrics(
                        rs.getInt("scheme_id"),
                        rs.getString("scheme_name"),
                        (Integer) rs.getObject("status"),
                        rs.getInt("submission_days"),
                        rs.getLong("total_water_supplied"),
                        (Integer) rs.getObject("immediate_parent_lgd_id"),
                        rs.getString("immediate_parent_lgd_c_name"),
                        rs.getString("immediate_parent_lgd_title"),
                        (Integer) rs.getObject("immediate_parent_department_id"),
                        rs.getString("immediate_parent_department_c_name"),
                        rs.getString("immediate_parent_department_title")),
                parentDepartmentId,
                startDate,
                endDate,
                ChronoUnit.DAYS.between(startDate, endDate) + 1,
                topSchemeCount);
    }

    public List<SchemeRegularityListMetrics> getSchemeRegionReportByLgd(
            Integer parentLgdId, LocalDate startDate, LocalDate endDate) {
        Integer lgdLevel = getLgdLevel(parentLgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("parent_lgd_id not found in dim_lgd_location_table: " + parentLgdId);
        }
        String schemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);

        String sql = String.format("""
                WITH schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.scheme_name,
                        s.status
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%1$s = ?
                ),
                scheme_days AS (
                    SELECT
                        m.scheme_id,
                        COUNT(DISTINCT CASE WHEN m.confirmed_reading > 0 THEN m.reading_date END)::int AS supply_days,
                        COUNT(DISTINCT CASE WHEN m.confirmed_reading >= 0 THEN m.reading_date END)::int AS submission_days
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_scope ss
                        ON ss.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                    GROUP BY m.scheme_id
                )
                SELECT
                    ss.scheme_id,
                    ss.scheme_name,
                    ss.status,
                    COALESCE(sd.supply_days, 0)::int AS supply_days,
                    COALESCE(sd.submission_days, 0)::int AS submission_days
                FROM schemes_in_scope ss
                LEFT JOIN scheme_days sd
                    ON sd.scheme_id = ss.scheme_id
                ORDER BY ss.scheme_id
                """, schemeLgdColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SchemeRegularityListMetrics(
                        rs.getInt("scheme_id"),
                        rs.getString("scheme_name"),
                        (Integer) rs.getObject("status"),
                        rs.getInt("supply_days"),
                        rs.getInt("submission_days")),
                parentLgdId,
                startDate,
                endDate);
    }

    public List<SchemeRegularityListMetrics> getSchemeRegionReportByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate) {
        Integer departmentLevel = getDepartmentLevel(parentDepartmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException(
                    "parent_department_id not found in dim_department_location_table: " + parentDepartmentId);
        }
        String schemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);

        String sql = String.format("""
                WITH schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.scheme_name,
                        s.status
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%1$s = ?
                ),
                scheme_days AS (
                    SELECT
                        m.scheme_id,
                        COUNT(DISTINCT CASE WHEN m.confirmed_reading > 0 THEN m.reading_date END)::int AS supply_days,
                        COUNT(DISTINCT CASE WHEN m.confirmed_reading >= 0 THEN m.reading_date END)::int AS submission_days
                    FROM analytics_schema.fact_meter_reading_table m
                    JOIN schemes_in_scope ss
                        ON ss.scheme_id = m.scheme_id
                    WHERE m.reading_date BETWEEN ? AND ?
                    GROUP BY m.scheme_id
                )
                SELECT
                    ss.scheme_id,
                    ss.scheme_name,
                    ss.status,
                    COALESCE(sd.supply_days, 0)::int AS supply_days,
                    COALESCE(sd.submission_days, 0)::int AS submission_days
                FROM schemes_in_scope ss
                LEFT JOIN scheme_days sd
                    ON sd.scheme_id = ss.scheme_id
                ORDER BY ss.scheme_id
                """, schemeDepartmentColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SchemeRegularityListMetrics(
                        rs.getInt("scheme_id"),
                        rs.getString("scheme_name"),
                        (Integer) rs.getObject("status"),
                        rs.getInt("supply_days"),
                        rs.getInt("submission_days")),
                parentDepartmentId,
                startDate,
                endDate);
    }

    public String getParentLgdCNameByLgd(Integer lgdId) {
        Integer lgdLevel = getLgdLevel(lgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("lgd_id not found in dim_lgd_location_table: " + lgdId);
        }
        String schemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);

        String sql = String.format("""
                SELECT MAX(l.lgd_c_name) AS parent_lgd_c_name
                FROM analytics_schema.dim_scheme_table s
                LEFT JOIN analytics_schema.dim_lgd_location_table l
                    ON l.lgd_id = s.parent_lgd_location_id
                WHERE s.%1$s = ?
                """, schemeLgdColumn);

        return jdbcTemplate.queryForObject(sql, String.class, lgdId);
    }

    public String getParentLgdTitleByLgd(Integer lgdId) {
        Integer lgdLevel = getLgdLevel(lgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("lgd_id not found in dim_lgd_location_table: " + lgdId);
        }
        String schemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);

        String sql = String.format("""
                SELECT MAX(l.title) AS parent_lgd_title
                FROM analytics_schema.dim_scheme_table s
                LEFT JOIN analytics_schema.dim_lgd_location_table l
                    ON l.lgd_id = s.parent_lgd_location_id
                WHERE s.%1$s = ?
                """, schemeLgdColumn);

        return jdbcTemplate.queryForObject(sql, String.class, lgdId);
    }

    public String getParentDepartmentCNameByDepartment(Integer departmentId) {
        Integer departmentLevel = getDepartmentLevel(departmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException("department_id not found in dim_department_location_table: " + departmentId);
        }
        String schemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);

        String sql = String.format("""
                SELECT MAX(d.department_c_name) AS parent_department_c_name
                FROM analytics_schema.dim_scheme_table s
                LEFT JOIN analytics_schema.dim_department_location_table d
                    ON d.department_id = s.parent_department_location_id
                WHERE s.%1$s = ?
                """, schemeDepartmentColumn);

        return jdbcTemplate.queryForObject(sql, String.class, departmentId);
    }

    public String getParentDepartmentTitleByDepartment(Integer departmentId) {
        Integer departmentLevel = getDepartmentLevel(departmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException("department_id not found in dim_department_location_table: " + departmentId);
        }
        String schemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);

        String sql = String.format("""
                SELECT MAX(d.title) AS parent_department_title
                FROM analytics_schema.dim_scheme_table s
                LEFT JOIN analytics_schema.dim_department_location_table d
                    ON d.department_id = s.parent_department_location_id
                WHERE s.%1$s = ?
                """, schemeDepartmentColumn);

        return jdbcTemplate.queryForObject(sql, String.class, departmentId);
    }

    public List<SchemeWaterSupplyMetrics> getAverageWaterSupplyPerCurrentRegion(
            Integer tenantId, LocalDate startDate, LocalDate endDate) {
        long daysInRange = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysInRange <= 0) {
            return List.of();
        }
        String sql = """
                WITH schemes_in_tenant AS (
                    SELECT
                        s.scheme_id,
                        s.scheme_name,
                        s.house_hold_count
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.tenant_id = ?
                      AND s.house_hold_count IS NOT NULL
                      AND s.house_hold_count > 0
                )
                SELECT
                    s.scheme_id,
                    s.scheme_name,
                    s.house_hold_count,
                    COALESCE(SUM(CASE WHEN m.confirmed_reading > 0 THEN m.confirmed_reading ELSE 0 END), 0)::bigint
                        AS total_water_supplied_liters,
                    COALESCE(COUNT(DISTINCT CASE WHEN m.confirmed_reading > 0 THEN m.reading_date END), 0)::int
                        AS supply_days
                FROM schemes_in_tenant s
                LEFT JOIN analytics_schema.fact_meter_reading_table m
                    ON m.scheme_id = s.scheme_id
                    AND m.tenant_id = ?
                    AND m.reading_date BETWEEN ? AND ?
                GROUP BY s.scheme_id, s.scheme_name, s.house_hold_count
                ORDER BY s.scheme_id
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SchemeWaterSupplyMetrics(
                        rs.getInt("scheme_id"),
                        rs.getString("scheme_name"),
                        rs.getInt("house_hold_count"),
                        rs.getLong("total_water_supplied_liters"),
                        rs.getInt("supply_days"),
                        BigDecimal.valueOf(rs.getLong("total_water_supplied_liters"))
                                .divide(
                                        BigDecimal.valueOf((long) rs.getInt("house_hold_count") * daysInRange),
                                        4,
                                        java.math.RoundingMode.HALF_UP)),
                tenantId,
                tenantId,
                startDate,
                endDate);
    }

    public List<ChildRegionWaterSupplyMetrics> getAverageWaterSupplyPerNation(
            LocalDate startDate, LocalDate endDate) {
        String sql = """
                WITH water_by_scheme AS (
                    SELECT
                        m.tenant_id,
                        m.scheme_id,
                        COALESCE(SUM(CASE WHEN m.confirmed_reading > 0 THEN m.confirmed_reading ELSE 0 END), 0)::bigint
                            AS total_water_supplied_liters
                    FROM analytics_schema.fact_meter_reading_table m
                    WHERE m.reading_date BETWEEN ? AND ?
                    GROUP BY m.tenant_id, m.scheme_id
                )
                SELECT
                    t.tenant_id,
                    t.state_code,
                    t.title,
                    COALESCE(SUM(COALESCE(s.house_hold_count, 0)), 0)::int AS total_household_count,
                    COALESCE(SUM(w.total_water_supplied_liters), 0)::bigint AS total_water_supplied_liters,
                    COALESCE(COUNT(s.scheme_id), 0)::int AS scheme_count,
                    CASE
                        WHEN COUNT(s.scheme_id) > 0
                            THEN ROUND(COALESCE(SUM(w.total_water_supplied_liters), 0)::numeric / COUNT(s.scheme_id), 4)
                        ELSE 0::numeric
                    END AS avg_water_supply_per_scheme
                FROM analytics_schema.dim_tenant_table t
                LEFT JOIN analytics_schema.dim_scheme_table s
                    ON s.tenant_id = t.tenant_id
                LEFT JOIN water_by_scheme w
                    ON w.tenant_id = s.tenant_id
                    AND w.scheme_id = s.scheme_id
                GROUP BY t.tenant_id, t.state_code, t.title
                ORDER BY t.tenant_id
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ChildRegionWaterSupplyMetrics(
                        rs.getInt("tenant_id"),
                        rs.getString("state_code"),
                        null,
                        null,
                        rs.getString("title"),
                        rs.getInt("total_household_count"),
                        rs.getLong("total_water_supplied_liters"),
                        rs.getInt("scheme_count"),
                        rs.getBigDecimal("avg_water_supply_per_scheme")),
                startDate,
                endDate);
    }

    public List<StateSchemeRegularityMetrics> getStateWiseRegularityMetrics(
            LocalDate startDate, LocalDate endDate) {
        String sql = """
                WITH supply_days_by_scheme AS (
                    SELECT
                        m.tenant_id,
                        m.scheme_id,
                        COUNT(DISTINCT m.reading_date)::int AS supply_days
                    FROM analytics_schema.fact_meter_reading_table m
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading > 0
                    GROUP BY m.tenant_id, m.scheme_id
                )
                SELECT
                    t.tenant_id,
                    t.state_code,
                    t.title,
                    COALESCE(COUNT(s.scheme_id), 0)::int AS scheme_count,
                    COALESCE(SUM(sd.supply_days), 0)::int AS total_supply_days
                FROM analytics_schema.dim_tenant_table t
                LEFT JOIN analytics_schema.dim_scheme_table s
                    ON s.tenant_id = t.tenant_id
                LEFT JOIN supply_days_by_scheme sd
                    ON sd.tenant_id = s.tenant_id
                    AND sd.scheme_id = s.scheme_id
                GROUP BY t.tenant_id, t.state_code, t.title
                ORDER BY t.tenant_id
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new StateSchemeRegularityMetrics(
                        rs.getInt("tenant_id"),
                        rs.getString("state_code"),
                        rs.getString("title"),
                        rs.getInt("scheme_count"),
                        rs.getInt("total_supply_days")),
                startDate,
                endDate);
    }

    public List<StateReadingSubmissionMetrics> getStateWiseReadingSubmissionMetrics(
            LocalDate startDate, LocalDate endDate) {
        String sql = """
                WITH submission_days_by_scheme AS (
                    SELECT
                        m.tenant_id,
                        m.scheme_id,
                        COUNT(DISTINCT m.reading_date)::int AS submission_days
                    FROM analytics_schema.fact_meter_reading_table m
                    WHERE m.reading_date BETWEEN ? AND ?
                      AND m.confirmed_reading >= 0
                    GROUP BY m.tenant_id, m.scheme_id
                )
                SELECT
                    t.tenant_id,
                    t.state_code,
                    t.title,
                    COALESCE(COUNT(s.scheme_id), 0)::int AS scheme_count,
                    COALESCE(SUM(sd.submission_days), 0)::int AS total_submission_days
                FROM analytics_schema.dim_tenant_table t
                LEFT JOIN analytics_schema.dim_scheme_table s
                    ON s.tenant_id = t.tenant_id
                LEFT JOIN submission_days_by_scheme sd
                    ON sd.tenant_id = s.tenant_id
                    AND sd.scheme_id = s.scheme_id
                GROUP BY t.tenant_id, t.state_code, t.title
                ORDER BY t.tenant_id
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new StateReadingSubmissionMetrics(
                        rs.getInt("tenant_id"),
                        rs.getString("state_code"),
                        rs.getString("title"),
                        rs.getInt("scheme_count"),
                        rs.getInt("total_submission_days")),
                startDate,
                endDate);
    }

    public List<OutageReasonSchemeCount> getOverallOutageReasonSchemeCount(
            LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT
                    f.outage_reason,
                    COUNT(DISTINCT f.scheme_id)::int AS scheme_count
                FROM analytics_schema.fact_water_quantity_table f
                WHERE f.outage_reason IS NOT NULL
                  AND f.date BETWEEN ? AND ?
                GROUP BY f.outage_reason
                ORDER BY f.outage_reason
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new OutageReasonSchemeCount(
                        (Integer) rs.getObject("outage_reason"),
                        rs.getInt("scheme_count")),
                startDate,
                endDate);
    }

    public List<ChildRegionWaterSupplyMetrics> getAverageWaterSupplyPerCurrentRegionByLgd(
            Integer tenantId, Integer lgdId, LocalDate startDate, LocalDate endDate) {
        Integer lgdLevel = getLgdLevel(lgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("lgd_id not found in dim_lgd_location_table: " + lgdId);
        }
        if (lgdLevel >= 6) {
            throw new IllegalArgumentException("No child LGD level available for parent_lgd_id: " + lgdId);
        }

        int childLevel = lgdLevel + 1;
        String parentSchemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);
        String childSchemeLgdColumn = resolveSchemeLgdColumn(childLevel);
        String childRegionParentLgdColumn = resolveChildRegionLgdParentColumn(lgdLevel);

        String sql = String.format("""
                WITH child_regions AS (
                    SELECT
                        l.lgd_id AS child_lgd_id,
                        l.title
                    FROM analytics_schema.dim_lgd_location_table l
                    WHERE l.tenant_id = ?
                      AND l.lgd_level = ?
                      AND l.%1$s = ?
                ),
                schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.%2$s AS child_lgd_id,
                        COALESCE(s.house_hold_count, 0) AS house_hold_count
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.tenant_id = ?
                      AND s.%3$s = ?
                ),
                water_by_scheme AS (
                    SELECT
                        m.scheme_id,
                        COALESCE(SUM(CASE WHEN m.confirmed_reading > 0 THEN m.confirmed_reading ELSE 0 END), 0)::bigint
                            AS total_water_supplied_liters
                    FROM analytics_schema.fact_meter_reading_table m
                    WHERE m.tenant_id = ?
                      AND m.reading_date BETWEEN ? AND ?
                    GROUP BY m.scheme_id
                )
                SELECT
                    c.child_lgd_id AS lgd_id,
                    c.title,
                    COALESCE(SUM(s.house_hold_count), 0)::int AS total_household_count,
                    COALESCE(SUM(w.total_water_supplied_liters), 0)::bigint AS total_water_supplied_liters,
                    COALESCE(COUNT(s.scheme_id), 0)::int AS scheme_count,
                    CASE
                        WHEN COUNT(s.scheme_id) > 0
                            THEN ROUND(COALESCE(SUM(w.total_water_supplied_liters), 0)::numeric / COUNT(s.scheme_id), 4)
                        ELSE 0::numeric
                    END AS avg_water_supply_per_scheme
                FROM child_regions c
                LEFT JOIN schemes_in_scope s
                    ON s.child_lgd_id = c.child_lgd_id
                LEFT JOIN water_by_scheme w
                    ON w.scheme_id = s.scheme_id
                GROUP BY c.child_lgd_id, c.title
                ORDER BY c.child_lgd_id
                """, childRegionParentLgdColumn, childSchemeLgdColumn, parentSchemeLgdColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ChildRegionWaterSupplyMetrics(
                        null,
                        null,
                        rs.getInt("lgd_id"),
                        null,
                        rs.getString("title"),
                        rs.getInt("total_household_count"),
                        rs.getLong("total_water_supplied_liters"),
                        rs.getInt("scheme_count"),
                        rs.getBigDecimal("avg_water_supply_per_scheme")),
                tenantId,
                childLevel,
                lgdId,
                tenantId,
                lgdId,
                tenantId,
                startDate,
                endDate);
    }

    public List<ChildRegionWaterSupplyMetrics> getAverageWaterSupplyPerCurrentRegionByDepartment(
            Integer tenantId, Integer parentDepartmentId, LocalDate startDate, LocalDate endDate) {
        Integer departmentLevel = getDepartmentLevel(parentDepartmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException(
                    "parent_department_id not found in dim_department_location_table: " + parentDepartmentId);
        }
        if (departmentLevel >= 6) {
            throw new IllegalArgumentException("No child department level available for parent_department_id: " + parentDepartmentId);
        }

        int childLevel = departmentLevel + 1;
        String parentSchemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);
        String childSchemeDepartmentColumn = resolveSchemeDepartmentColumn(childLevel);
        String childRegionParentDepartmentColumn = resolveChildRegionDepartmentParentColumn(departmentLevel);

        String sql = String.format("""
                WITH child_regions AS (
                    SELECT
                        d.department_id AS child_department_id,
                        d.title
                    FROM analytics_schema.dim_department_location_table d
                    WHERE d.tenant_id = ?
                      AND d.department_level = ?
                      AND d.%1$s = ?
                ),
                schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.%2$s AS child_department_id,
                        COALESCE(s.house_hold_count, 0) AS house_hold_count
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.tenant_id = ?
                      AND s.%3$s = ?
                ),
                water_by_scheme AS (
                    SELECT
                        m.scheme_id,
                        COALESCE(SUM(CASE WHEN m.confirmed_reading > 0 THEN m.confirmed_reading ELSE 0 END), 0)::bigint
                            AS total_water_supplied_liters
                    FROM analytics_schema.fact_meter_reading_table m
                    WHERE m.tenant_id = ?
                      AND m.reading_date BETWEEN ? AND ?
                    GROUP BY m.scheme_id
                )
                SELECT
                    c.child_department_id AS department_id,
                    c.title,
                    COALESCE(SUM(s.house_hold_count), 0)::int AS total_household_count,
                    COALESCE(SUM(w.total_water_supplied_liters), 0)::bigint AS total_water_supplied_liters,
                    COALESCE(COUNT(s.scheme_id), 0)::int AS scheme_count,
                    CASE
                        WHEN COUNT(s.scheme_id) > 0
                            THEN ROUND(COALESCE(SUM(w.total_water_supplied_liters), 0)::numeric / COUNT(s.scheme_id), 4)
                        ELSE 0::numeric
                    END AS avg_water_supply_per_scheme
                FROM child_regions c
                LEFT JOIN schemes_in_scope s
                    ON s.child_department_id = c.child_department_id
                LEFT JOIN water_by_scheme w
                    ON w.scheme_id = s.scheme_id
                GROUP BY c.child_department_id, c.title
                ORDER BY c.child_department_id
                """, childRegionParentDepartmentColumn, childSchemeDepartmentColumn, parentSchemeDepartmentColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ChildRegionWaterSupplyMetrics(
                        null,
                        null,
                        null,
                        rs.getInt("department_id"),
                        rs.getString("title"),
                        rs.getInt("total_household_count"),
                        rs.getLong("total_water_supplied_liters"),
                        rs.getInt("scheme_count"),
                        rs.getBigDecimal("avg_water_supply_per_scheme")),
                tenantId,
                childLevel,
                parentDepartmentId,
                tenantId,
                parentDepartmentId,
                tenantId,
                startDate,
                endDate);
    }

    public List<ChildRegionWaterQuantityMetrics> getRegionWiseWaterQuantityByLgd(
            Integer parentLgdId, LocalDate startDate, LocalDate endDate) {
        Integer parentLgdLevel = getLgdLevel(parentLgdId);
        if (parentLgdLevel == null) {
            throw new IllegalArgumentException("parent_lgd_id not found in dim_lgd_location_table: " + parentLgdId);
        }
        if (parentLgdLevel >= 6) {
            throw new IllegalArgumentException("No child LGD level available for parent_lgd_id: " + parentLgdId);
        }

        int childLevel = parentLgdLevel + 1;
        String parentSchemeLgdColumn = resolveSchemeLgdColumn(parentLgdLevel);
        String childSchemeLgdColumn = resolveSchemeLgdColumn(childLevel);
        String childRegionParentLgdColumn = resolveChildRegionLgdParentColumn(parentLgdLevel);

        String sql = String.format("""
                WITH child_regions AS (
                    SELECT
                        l.lgd_id AS child_lgd_id,
                        l.title
                    FROM analytics_schema.dim_lgd_location_table l
                    WHERE l.lgd_level = ?
                      AND l.%1$s = ?
                ),
                schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.%2$s AS child_lgd_id,
                        COALESCE(s.house_hold_count, 0) AS house_hold_count
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%3$s = ?
                ),
                ewater_by_scheme AS (
                    SELECT
                        f.scheme_id,
                        COALESCE(SUM(f.water_quantity), 0)::bigint AS total_ewater_quantity
                    FROM analytics_schema.fact_water_quantity_table f
                    WHERE f.date BETWEEN ? AND ?
                    GROUP BY f.scheme_id
                )
                SELECT
                    c.child_lgd_id AS lgd_id,
                    c.title,
                    COALESCE(SUM(s.house_hold_count), 0)::int AS household_count,
                    COALESCE(SUM(w.total_ewater_quantity), 0)::bigint AS ewater_quantity
                FROM child_regions c
                LEFT JOIN schemes_in_scope s
                    ON s.child_lgd_id = c.child_lgd_id
                LEFT JOIN ewater_by_scheme w
                    ON w.scheme_id = s.scheme_id
                GROUP BY c.child_lgd_id, c.title
                ORDER BY c.child_lgd_id
                """, childRegionParentLgdColumn, childSchemeLgdColumn, parentSchemeLgdColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ChildRegionWaterQuantityMetrics(
                        rs.getInt("lgd_id"),
                        null,
                        rs.getString("title"),
                        rs.getLong("ewater_quantity"),
                        rs.getInt("household_count")),
                childLevel,
                parentLgdId,
                parentLgdId,
                startDate,
                endDate);
    }

    public List<ChildRegionWaterQuantityMetrics> getRegionWiseWaterQuantityByDepartment(
            Integer parentDepartmentId, LocalDate startDate, LocalDate endDate) {
        Integer parentDepartmentLevel = getDepartmentLevel(parentDepartmentId);
        if (parentDepartmentLevel == null) {
            throw new IllegalArgumentException(
                    "parent_department_id not found in dim_department_location_table: " + parentDepartmentId);
        }
        if (parentDepartmentLevel >= 6) {
            throw new IllegalArgumentException("No child department level available for parent_department_id: " + parentDepartmentId);
        }

        int childLevel = parentDepartmentLevel + 1;
        String parentSchemeDepartmentColumn = resolveSchemeDepartmentColumn(parentDepartmentLevel);
        String childSchemeDepartmentColumn = resolveSchemeDepartmentColumn(childLevel);
        String childRegionParentDepartmentColumn = resolveChildRegionDepartmentParentColumn(parentDepartmentLevel);

        String sql = String.format("""
                WITH child_regions AS (
                    SELECT
                        d.department_id AS child_department_id,
                        d.title
                    FROM analytics_schema.dim_department_location_table d
                    WHERE d.department_level = ?
                      AND d.%1$s = ?
                ),
                schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        s.%2$s AS child_department_id,
                        COALESCE(s.house_hold_count, 0) AS house_hold_count
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%3$s = ?
                ),
                ewater_by_scheme AS (
                    SELECT
                        f.scheme_id,
                        COALESCE(SUM(f.water_quantity), 0)::bigint AS total_ewater_quantity
                    FROM analytics_schema.fact_water_quantity_table f
                    WHERE f.date BETWEEN ? AND ?
                    GROUP BY f.scheme_id
                )
                SELECT
                    c.child_department_id AS department_id,
                    c.title,
                    COALESCE(SUM(s.house_hold_count), 0)::int AS household_count,
                    COALESCE(SUM(w.total_ewater_quantity), 0)::bigint AS ewater_quantity
                FROM child_regions c
                LEFT JOIN schemes_in_scope s
                    ON s.child_department_id = c.child_department_id
                LEFT JOIN ewater_by_scheme w
                    ON w.scheme_id = s.scheme_id
                GROUP BY c.child_department_id, c.title
                ORDER BY c.child_department_id
                """, childRegionParentDepartmentColumn, childSchemeDepartmentColumn, parentSchemeDepartmentColumn);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ChildRegionWaterQuantityMetrics(
                        null,
                        rs.getInt("department_id"),
                        rs.getString("title"),
                        rs.getLong("ewater_quantity"),
                        rs.getInt("household_count")),
                childLevel,
                parentDepartmentId,
                parentDepartmentId,
                startDate,
                endDate);
    }

    public List<PeriodicWaterQuantityMetrics> getPeriodicWaterQuantityByLgdId(
            Integer lgdId, LocalDate startDate, LocalDate endDate, PeriodScale scale) {
        Integer lgdLevel = getLgdLevel(lgdId);
        if (lgdLevel == null) {
            throw new IllegalArgumentException("lgd_id not found in dim_lgd_location_table: " + lgdId);
        }
        String schemeLgdColumn = resolveSchemeLgdColumn(lgdLevel);
        return getPeriodicWaterQuantityMetrics(schemeLgdColumn, lgdId, startDate, endDate, scale);
    }

    public List<PeriodicWaterQuantityMetrics> getPeriodicWaterQuantityByDepartment(
            Integer departmentId, LocalDate startDate, LocalDate endDate, PeriodScale scale) {
        Integer departmentLevel = getDepartmentLevel(departmentId);
        if (departmentLevel == null) {
            throw new IllegalArgumentException("department_id not found in dim_department_location_table: " + departmentId);
        }
        String schemeDepartmentColumn = resolveSchemeDepartmentColumn(departmentLevel);
        return getPeriodicWaterQuantityMetrics(schemeDepartmentColumn, departmentId, startDate, endDate, scale);
    }

    private List<PeriodicWaterQuantityMetrics> getPeriodicWaterQuantityMetrics(
            String schemeLocationColumn,
            Object locationId,
            LocalDate startDate,
            LocalDate endDate,
            PeriodScale scale) {
        PeriodSqlParts sqlParts = buildPeriodSqlParts(scale);
        String sql = String.format("""
                WITH schemes_in_scope AS (
                    SELECT
                        s.scheme_id,
                        COALESCE(s.house_hold_count, 0)::int AS house_hold_count
                    FROM analytics_schema.dim_scheme_table s
                    WHERE s.%1$s = ?
                ),
                periods AS (
                    SELECT DISTINCT
                        %2$s AS period_start_date,
                        %3$s AS period_end_date,
                        %4$s AS scope
                    FROM generate_series(?::date, ?::date, INTERVAL '1 day') AS g(day_date)
                ),
                water_by_period AS (
                    SELECT
                        %5$s AS period_start_date,
                        AVG(f.water_quantity::numeric) AS avg_water_quantity
                    FROM analytics_schema.fact_water_quantity_table f
                    JOIN schemes_in_scope s
                        ON s.scheme_id = f.scheme_id
                    WHERE f.date BETWEEN ? AND ?
                    GROUP BY %5$s
                ),
                household_total AS (
                    SELECT COALESCE(SUM(house_hold_count), 0)::int AS household_count
                    FROM schemes_in_scope
                )
                SELECT
                    p.period_start_date,
                    p.period_end_date,
                    p.scope,
                    COALESCE(w.avg_water_quantity, 0)::numeric AS average_water_quantity,
                    h.household_count
                FROM periods p
                LEFT JOIN water_by_period w
                    ON w.period_start_date = p.period_start_date
                CROSS JOIN household_total h
                ORDER BY p.period_start_date
                """,
                schemeLocationColumn,
                sqlParts.periodStartFromSeries(),
                sqlParts.periodEndFromSeries(),
                sqlParts.periodLabelFromSeries(),
                sqlParts.periodStartFromFact());

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new PeriodicWaterQuantityMetrics(
                        rs.getObject("period_start_date", LocalDate.class),
                        rs.getObject("period_end_date", LocalDate.class),
                        rs.getString("scope"),
                        rs.getBigDecimal("average_water_quantity").setScale(4, RoundingMode.HALF_UP),
                        rs.getInt("household_count")),
                locationId,
                startDate,
                endDate,
                startDate,
                endDate);
    }

    public Integer getLgdLevel(Integer lgdId) {
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

    private PeriodSqlParts buildPeriodSqlParts(PeriodScale scale) {
        return switch (scale) {
            case DAY -> new PeriodSqlParts(
                    "g.day_date::date",
                    "g.day_date::date",
                    "TO_CHAR(g.day_date::date, 'YYYY-MM-DD')",
                    "f.date::date");
            case WEEK -> new PeriodSqlParts(
                    "DATE_TRUNC('week', g.day_date)::date",
                    "(DATE_TRUNC('week', g.day_date)::date + 6)",
                    "TO_CHAR(DATE_TRUNC('week', g.day_date)::date, 'IYYY-\"W\"IW')",
                    "DATE_TRUNC('week', f.date)::date");
            case MONTH -> new PeriodSqlParts(
                    "DATE_TRUNC('month', g.day_date)::date",
                    "(DATE_TRUNC('month', g.day_date)::date + INTERVAL '1 month - 1 day')::date",
                    "TO_CHAR(DATE_TRUNC('month', g.day_date)::date, 'YYYY-MM')",
                    "DATE_TRUNC('month', f.date)::date");
        };
    }

    public Integer getDepartmentLevel(Integer parentDepartmentId) {
        String sql = """
                SELECT d.department_level
                FROM analytics_schema.dim_department_location_table d
                WHERE d.department_id = ?
                LIMIT 1
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> (Integer) rs.getObject("department_level"), parentDepartmentId)
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

    private String resolveSchemeDepartmentColumn(Integer departmentLevel) {
        return switch (departmentLevel) {
            case 0 -> "parent_department_location_id";
            case 1 -> "level_1_dept_id";
            case 2 -> "level_2_dept_id";
            case 3 -> "level_3_dept_id";
            case 4 -> "level_4_dept_id";
            case 5 -> "level_5_dept_id";
            case 6 -> "level_6_dept_id";
            default -> throw new IllegalArgumentException("Unsupported department_level: " + departmentLevel);
        };
    }

    private String resolveChildRegionLgdParentColumn(Integer parentLgdLevel) {
        return switch (parentLgdLevel) {
            case 0, 1 -> "level_1_lgd_id";
            case 2 -> "level_2_lgd_id";
            case 3 -> "level_3_lgd_id";
            case 4 -> "level_4_lgd_id";
            case 5 -> "level_5_lgd_id";
            default -> throw new IllegalArgumentException("Unsupported parent lgd_level for child lookup: " + parentLgdLevel);
        };
    }

    private String resolveChildRegionDepartmentParentColumn(Integer parentDepartmentLevel) {
        return switch (parentDepartmentLevel) {
            case 0, 1 -> "level_1_dept_id";
            case 2 -> "level_2_dept_id";
            case 3 -> "level_3_dept_id";
            case 4 -> "level_4_dept_id";
            case 5 -> "level_5_dept_id";
            default -> throw new IllegalArgumentException(
                    "Unsupported parent department_level for child lookup: " + parentDepartmentLevel);
        };
    }

    public record SchemeRegularityMetrics(int schemeCount, int totalSupplyDays) {
    }

    public record SchemeWaterSupplyMetrics(
            Integer schemeId,
            String schemeName,
            Integer householdCount,
            Long totalWaterSuppliedLiters,
            Integer supplyDays,
            BigDecimal averageLitersPerHousehold) {
    }

    public record ChildRegionWaterSupplyMetrics(
            Integer tenantId,
            String stateCode,
            Integer lgdId,
            Integer departmentId,
            String title,
            Integer totalHouseholdCount,
            Long totalWaterSuppliedLiters,
            Integer schemeCount,
            BigDecimal avgWaterSupplyPerScheme) {
    }

    public record ChildRegionWaterQuantityMetrics(
            Integer lgdId,
            Integer departmentId,
            String title,
            Long waterQuantity,
            Integer householdCount) {
    }

    public record ChildRegionSchemeRegularityMetrics(
            Integer lgdId,
            Integer departmentId,
            String title,
            Integer schemeCount,
            Integer totalSupplyDays,
            BigDecimal averageRegularity) {
    }

    public record ChildRegionReadingSubmissionMetrics(
            Integer lgdId,
            Integer departmentId,
            String title,
            Integer schemeCount,
            Integer totalSubmissionDays,
            BigDecimal readingSubmissionRate) {
    }

    public record StateSchemeRegularityMetrics(
            Integer tenantId,
            String stateCode,
            String title,
            Integer schemeCount,
            Integer totalSupplyDays) {
    }

    public record StateReadingSubmissionMetrics(
            Integer tenantId,
            String stateCode,
            String title,
            Integer schemeCount,
            Integer totalSubmissionDays) {
    }

    private record PeriodSqlParts(
            String periodStartFromSeries,
            String periodEndFromSeries,
            String periodLabelFromSeries,
            String periodStartFromFact) {
    }

    public record OutageReasonSchemeCount(Integer outageReason, Integer schemeCount) {
    }

    public record ChildRegionRef(Integer lgdId, Integer departmentId, String title) {
    }

    public record ChildRegionOutageReasonSchemeCount(
            Integer lgdId,
            Integer departmentId,
            Integer outageReason,
            Integer schemeCount) {
    }

    public record SchemeStatusCount(Integer activeSchemeCount, Integer inactiveSchemeCount) {
    }

    public record SchemeSubmissionMetrics(
            Integer schemeId,
            String schemeName,
            Integer status,
            Integer submissionDays,
            Long totalWaterSupplied,
            Integer immediateParentLgdId,
            String immediateParentLgdCName,
            String immediateParentLgdTitle,
            Integer immediateParentDepartmentId,
            String immediateParentDepartmentCName,
            String immediateParentDepartmentTitle) {
    }

    public record SchemeRegularityListMetrics(
            Integer schemeId,
            String schemeName,
            Integer status,
            Integer supplyDays,
            Integer submissionDays) {
    }

    public record SubmissionStatusCount(Integer compliantSubmissionCount, Integer anomalousSubmissionCount) {
    }

    public record DailyOutageReasonSchemeCount(
            LocalDate date,
            Integer outageReason,
            Integer schemeCount) {
    }

    public record DailySubmissionSchemeCount(
            LocalDate date,
            Integer submittedSchemeCount) {
    }

    public record PeriodicWaterQuantityMetrics(
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String scope,
            BigDecimal averageWaterQuantity,
            Integer householdCount) {
    }
}
