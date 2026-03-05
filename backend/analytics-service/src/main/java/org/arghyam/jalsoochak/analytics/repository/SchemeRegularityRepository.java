package org.arghyam.jalsoochak.analytics.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    public List<SchemeWaterSupplyMetrics> getAverageWaterSupplyPerScheme(
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

    public List<ChildRegionWaterSupplyMetrics> getAverageWaterSupplyPerSchemeByLgd(
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
                tenantId,
                startDate,
                endDate);
    }

    public List<ChildRegionWaterSupplyMetrics> getAverageWaterSupplyPerSchemeByDepartment(
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
                tenantId,
                startDate,
                endDate);
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

    private Integer getDepartmentLevel(Integer parentDepartmentId) {
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
}
