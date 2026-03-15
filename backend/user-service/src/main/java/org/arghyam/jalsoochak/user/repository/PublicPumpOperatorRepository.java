package org.arghyam.jalsoochak.user.repository;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorDetailsDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceRowDTO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PublicPumpOperatorRepository {

    private final JdbcTemplate jdbcTemplate;

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    public PumpOperatorDetailsDTO findPumpOperatorById(String schemaName, long pumpOperatorId) {
        validateSchemaName(schemaName);
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
                        MAX(fr.reading_at) AS last_submission_at,
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
                """, schemaName, schemaName, schemaName, schemaName, schemaName);
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
                        .status((Integer) rs.getObject("status"))
                        .schemeId((Integer) rs.getObject("scheme_id"))
                        .schemeName(rs.getString("scheme_name"))
                        .schemeLatitude((Double) rs.getObject("scheme_latitude"))
                        .schemeLongitude((Double) rs.getObject("scheme_longitude"))
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

    public PumpOperatorReadingComplianceDTO getReadingCompliance(String schemaName, long pumpOperatorId) {
        validateSchemaName(schemaName);

        // If the operator has no readings, lastSubmissionAt/confirmedReading will be null.
        String sql = String.format("""
                SELECT u.title AS name,
                       fr.last_submission_at,
                       fr.confirmed_reading
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                LEFT JOIN LATERAL (
                    SELECT reading_at AS last_submission_at, confirmed_reading
                    FROM %s.flow_reading_table
                    WHERE deleted_at IS NULL
                      AND created_by = u.id
                    ORDER BY reading_at DESC, id DESC
                    LIMIT 1
                ) fr ON true
                WHERE u.deleted_at IS NULL
                  AND u.id = ?
                  AND upper(COALESCE(ut.c_name, '')) = 'PUMP_OPERATOR'
                LIMIT 1
                """, schemaName, schemaName);

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
                    SELECT reading_at AS last_submission_at, confirmed_reading
                    FROM %s.flow_reading_table
                    WHERE deleted_at IS NULL
                      AND created_by = u.id
                    ORDER BY reading_at DESC, id DESC
                    LIMIT 1
                ) fr ON true
                WHERE u.deleted_at IS NULL
                  AND upper(COALESCE(ut.c_name, '')) = 'PUMP_OPERATOR'
                ORDER BY u.id DESC
                """, schemaName, schemaName);

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
