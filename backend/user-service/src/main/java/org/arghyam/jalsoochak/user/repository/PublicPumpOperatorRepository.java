package org.arghyam.jalsoochak.user.repository;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorDetailsDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceRowDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorSummaryDTO;
import org.arghyam.jalsoochak.user.dto.response.SchemePumpOperatorsDTO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Array;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PublicPumpOperatorRepository {

    private final JdbcTemplate jdbcTemplate;

    private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        return Integer.valueOf(o.toString());
    }

    private static Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        return Double.valueOf(o.toString());
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    private boolean columnExists(String schemaName, String tableName, String columnName) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = ?
                      AND table_name = ?
                      AND column_name = ?
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, schemaName, tableName, columnName);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * flow_reading_table time column differs across tenant schema versions:
     * - legacy: reading_at
     * - newer:  observation_time
     */
    private String resolveFlowReadingTimeColumn(String schemaName) {
        return columnExists(schemaName, "flow_reading_table", "observation_time") ? "observation_time" : "reading_at";
    }

    public PumpOperatorDetailsDTO findPumpOperatorById(String schemaName, long pumpOperatorId) {
        validateSchemaName(schemaName);
        String timeColumn = resolveFlowReadingTimeColumn(schemaName);
        String sql = String.format("""
                SELECT u.id,
                       u.uuid,
                       u.title,
                       u.email,
                       u.phone_number,
                       u.status,
                       ut.c_name AS role,
                       sch.scheme_id,
                       sch.scheme_name,
                       sch.latitude AS scheme_latitude,
                       sch.longitude AS scheme_longitude,
                       rs.last_submission_at,
                       rs.first_submission_date,
                       comp.total_days_since_first_submission,
                       rs.submitted_days,
                       comp.reporting_rate_percent,
                       comp.missed_submission_days
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                LEFT JOIN LATERAL (
                    SELECT sm.id AS scheme_id,
                           sm.scheme_name,
                           sm.latitude,
                           sm.longitude
                    FROM %s.user_scheme_mapping_table usm
                    JOIN %s.scheme_master_table sm
                      ON sm.id = usm.scheme_id
                     AND sm.deleted_at IS NULL
                    WHERE usm.deleted_at IS NULL
                      AND usm.user_id = u.id
                      AND usm.status = 1
                    ORDER BY usm.id DESC
                    LIMIT 1
                ) sch ON true
                LEFT JOIN LATERAL (
                    SELECT
                        MAX(fr.%s) AS last_submission_at,
                        MIN(fr.reading_date) AS first_submission_date,
                        COUNT(DISTINCT fr.reading_date) AS submitted_days
                    FROM %s.flow_reading_table fr
                    WHERE fr.deleted_at IS NULL
                      AND fr.created_by = u.id
                ) rs ON true
                LEFT JOIN LATERAL (
                    WITH bounds AS (
                        -- Guard: if there are no readings, rs.first_submission_date is NULL.
                        -- In that case we must not evaluate generate_series/date math with NULL bounds.
                        SELECT rs.first_submission_date AS start_date
                        WHERE rs.first_submission_date IS NOT NULL
                    ),
                    days AS (
                        -- Generate all dates from first submission date to today (inclusive) using integer offsets.
                        SELECT (bounds.start_date + gs) AS d
                        FROM bounds
                        JOIN generate_series(0, (CURRENT_DATE - bounds.start_date)) gs ON true
                    ),
                    reported AS (
                        SELECT DISTINCT fr.reading_date AS d
                        FROM %s.flow_reading_table fr
                        JOIN bounds ON true
                        WHERE fr.deleted_at IS NULL
                          AND fr.created_by = u.id
                          AND fr.reading_date BETWEEN bounds.start_date AND CURRENT_DATE
                    )
                    SELECT
                        (CURRENT_DATE - bounds.start_date + 1) AS total_days_since_first_submission,
                        ROUND(
                            (rs.submitted_days::numeric * 100.0) / NULLIF((CURRENT_DATE - bounds.start_date + 1), 0),
                            2
                        ) AS reporting_rate_percent,
                        (
                            SELECT array_agg(days.d ORDER BY days.d)
                            FROM days
                            LEFT JOIN reported ON reported.d = days.d
                            WHERE reported.d IS NULL
                        ) AS missed_submission_days
                    FROM bounds
                ) comp ON true
                WHERE u.deleted_at IS NULL
                  AND u.id = ?
                  AND upper(COALESCE(ut.c_name, '')) = 'PUMP_OPERATOR'
                LIMIT 1
                """, schemaName, schemaName, schemaName, schemaName, schemaName, timeColumn);
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Timestamp lastTs = (Timestamp) rs.getObject("last_submission_at");
                LocalDateTime lastSubmissionAt = lastTs == null ? null : lastTs.toLocalDateTime();

                java.sql.Date firstD = (java.sql.Date) rs.getObject("first_submission_date");
                LocalDate firstSubmissionDate = firstD == null ? null : firstD.toLocalDate();

                Number totalDaysN = (Number) rs.getObject("total_days_since_first_submission");
                Integer totalDays = totalDaysN == null ? null : totalDaysN.intValue();
                Number submittedDaysN = (Number) rs.getObject("submitted_days");
                Integer submittedDays = submittedDaysN == null ? null : submittedDaysN.intValue();

                List<LocalDate> missedDays = null;
                Array missedArr = (Array) rs.getObject("missed_submission_days");
                if (missedArr != null) {
                    Object raw = missedArr.getArray();
                    if (raw instanceof java.sql.Date[] sqlDates) {
                        missedDays = new ArrayList<>(sqlDates.length);
                        for (java.sql.Date d : sqlDates) {
                            missedDays.add(d == null ? null : d.toLocalDate());
                        }
                    } else if (raw instanceof Object[] objs) {
                        missedDays = new ArrayList<>(objs.length);
                        for (Object o : objs) {
                            if (o == null) {
                                missedDays.add(null);
                            } else if (o instanceof java.sql.Date d) {
                                missedDays.add(d.toLocalDate());
                            } else if (o instanceof LocalDate d) {
                                missedDays.add(d);
                            } else {
                                missedDays.add(LocalDate.parse(o.toString()));
                            }
                        }
                    }
                }

                return PumpOperatorDetailsDTO.builder()
                        .id(rs.getLong("id"))
                        .uuid(rs.getString("uuid"))
                        .name(rs.getString("title"))
                        .email(rs.getString("email"))
                        .phoneNumber(rs.getString("phone_number"))
                        .status(getNullableInt(rs, "status"))
                        .schemeId(getNullableInt(rs, "scheme_id"))
                        .schemeName(rs.getString("scheme_name"))
                        .schemeLatitude(getNullableDouble(rs, "scheme_latitude"))
                        .schemeLongitude(getNullableDouble(rs, "scheme_longitude"))
                        .lastSubmissionAt(lastSubmissionAt)
                        .firstSubmissionDate(firstSubmissionDate)
                        .totalDaysSinceFirstSubmission(totalDays)
                        .submittedDays(submittedDays)
                        .reportingRatePercent((BigDecimal) rs.getObject("reporting_rate_percent"))
                        .missedSubmissionDays(missedDays)
                        .build();
            }, pumpOperatorId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<SchemePumpOperatorsDTO> listPumpOperatorsByScheme(String schemaName, List<Long> schemeIds, String schemeName) {
        validateSchemaName(schemaName);

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("""
                WHERE usm.deleted_at IS NULL
                  AND usm.status = 1
                  AND ut.c_name = 'pump_operator'
                """);
        if (schemeIds != null && !schemeIds.isEmpty()) {
            where.append("\n  AND sm.id IN (");
            for (int i = 0; i < schemeIds.size(); i++) {
                if (i > 0) {
                    where.append(", ");
                }
                where.append("?");
                params.add(schemeIds.get(i));
            }
            where.append(")\n");
        }
        if (schemeName != null && !schemeName.trim().isBlank()) {
            where.append("\n  AND sm.scheme_name ILIKE ?\n");
            params.add("%" + schemeName.trim() + "%");
        }

        String sql = String.format("""
                SELECT t.scheme_id,
                       t.scheme_name,
                       t.user_id,
                       t.uuid,
                       t.name,
                       t.email,
                       t.phone_number,
                       t.status
                FROM (
                    SELECT DISTINCT ON (sm.id, u.id)
                           sm.id AS scheme_id,
                           sm.scheme_name AS scheme_name,
                           u.id AS user_id,
                           u.uuid AS uuid,
                           u.title AS name,
                           u.email AS email,
                           u.phone_number AS phone_number,
                           u.status AS status
                    FROM %s.user_scheme_mapping_table usm
                    JOIN %s.scheme_master_table sm
                      ON sm.id = usm.scheme_id
                     AND sm.deleted_at IS NULL
                    JOIN %s.user_table u
                      ON u.id = usm.user_id
                     AND u.deleted_at IS NULL
                    JOIN common_schema.user_type_master_table ut
                      ON ut.id = u.user_type
                    %s
                    ORDER BY sm.id, u.id, usm.id DESC
                ) t
                ORDER BY t.scheme_id ASC, t.name ASC, t.user_id ASC
                """, schemaName, schemaName, schemaName, where);

        record Row(long schemeId,
                   String schemeName,
                   long userId,
                   String uuid,
                   String name,
                   String email,
                   String phoneNumber,
                   Integer status) {
        }

        List<Row> rows = jdbcTemplate.query(sql, (rs, n) -> new Row(
                rs.getLong("scheme_id"),
                rs.getString("scheme_name"),
                rs.getLong("user_id"),
                rs.getString("uuid"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone_number"),
                getNullableInt(rs, "status")
        ), params.toArray());

        // Group while preserving query order.
        Map<Long, SchemePumpOperatorsDTO> grouped = new LinkedHashMap<>();
        for (Row r : rows) {
            SchemePumpOperatorsDTO existing = grouped.get(r.schemeId());
            PumpOperatorSummaryDTO op = PumpOperatorSummaryDTO.builder()
                    .id(r.userId())
                    .uuid(r.uuid())
                    .name(r.name())
                    .email(r.email())
                    .phoneNumber(r.phoneNumber())
                    .status(r.status())
                    .build();

            if (existing == null) {
                List<PumpOperatorSummaryDTO> ops = new ArrayList<>();
                ops.add(op);
                grouped.put(r.schemeId(), SchemePumpOperatorsDTO.builder()
                        .schemeId(r.schemeId())
                        .schemeName(r.schemeName())
                        .pumpOperators(ops)
                        .build());
            } else {
                // List is mutable because we constructed it above.
                existing.pumpOperators().add(op);
            }
        }

        return new ArrayList<>(grouped.values());
    }

    public PumpOperatorReadingComplianceDTO getReadingCompliance(String schemaName, long pumpOperatorId) {
        validateSchemaName(schemaName);
        String timeColumn = resolveFlowReadingTimeColumn(schemaName);

        // If the operator has no readings, lastSubmissionAt/confirmedReading will be null.
        String sql = String.format("""
                SELECT u.title AS name,
                       fr.last_submission_at,
                       fr.confirmed_reading
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                LEFT JOIN LATERAL (
                    SELECT %s AS last_submission_at, confirmed_reading
                    FROM %s.flow_reading_table
                    WHERE deleted_at IS NULL
                      AND created_by = u.id
                    ORDER BY %s DESC, id DESC
                    LIMIT 1
                ) fr ON true
                WHERE u.deleted_at IS NULL
                  AND u.id = ?
                  AND upper(COALESCE(ut.c_name, '')) = 'PUMP_OPERATOR'
                LIMIT 1
                """, schemaName, timeColumn, schemaName, timeColumn);

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Timestamp ts = (Timestamp) rs.getObject("last_submission_at");
                LocalDateTime lastSubmissionAt = ts == null ? null : ts.toLocalDateTime();
                BigDecimal confirmed = (BigDecimal) rs.getObject("confirmed_reading");
                return PumpOperatorReadingComplianceDTO.builder()
                        .name(rs.getString("name"))
                        .lastSubmissionAt(lastSubmissionAt)
                        .confirmedReading(confirmed)
                        .build();
            }, pumpOperatorId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<PumpOperatorReadingComplianceRowDTO> listReadingCompliance(String schemaName) {
        validateSchemaName(schemaName);
        String timeColumn = resolveFlowReadingTimeColumn(schemaName);

        String sql = String.format("""
                SELECT u.id,
                       u.uuid,
                       u.title AS name,
                       fr.last_submission_at,
                       fr.confirmed_reading
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                LEFT JOIN LATERAL (
                    SELECT %s AS last_submission_at, confirmed_reading
                    FROM %s.flow_reading_table
                    WHERE deleted_at IS NULL
                      AND created_by = u.id
                    ORDER BY %s DESC, id DESC
                    LIMIT 1
                ) fr ON true
                WHERE u.deleted_at IS NULL
                  AND upper(COALESCE(ut.c_name, '')) = 'PUMP_OPERATOR'
                ORDER BY u.id DESC
                """, schemaName, timeColumn, schemaName, timeColumn);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp ts = (Timestamp) rs.getObject("last_submission_at");
            LocalDateTime lastSubmissionAt = ts == null ? null : ts.toLocalDateTime();
            BigDecimal confirmed = (BigDecimal) rs.getObject("confirmed_reading");
            return PumpOperatorReadingComplianceRowDTO.builder()
                    .id(rs.getLong("id"))
                    .uuid(rs.getString("uuid"))
                    .name(rs.getString("name"))
                    .lastSubmissionAt(lastSubmissionAt)
                    .confirmedReading(confirmed)
                    .build();
        });
    }
}
