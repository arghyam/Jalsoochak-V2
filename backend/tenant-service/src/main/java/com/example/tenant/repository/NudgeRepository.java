package com.example.tenant.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * JdbcTemplate-based repository for nudge and escalation queries.
 * All methods accept the tenant schema name explicitly and validate it
 * before interpolating into SQL to prevent injection.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class NudgeRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Returns users who have an active scheme mapping but no flow reading for today.
     * These are candidates for a nudge notification.
     */
    public List<Map<String, Object>> findUsersWithNoUploadToday(String schema) {
        validateSchemaName(schema);
        String sql = String.format("""
                SELECT u.id, u.name, u.phone_number, usm.scheme_id
                FROM %s.user_scheme_mapping_table usm
                JOIN %s.user_table u ON u.id = usm.user_id
                LEFT JOIN %s.flow_reading_table fr
                    ON fr.scheme_id = usm.scheme_id
                    AND fr.created_by = u.id
                    AND fr.reading_date = CURRENT_DATE
                WHERE usm.status = 'ACTIVE' AND fr.id IS NULL
                """, schema, schema, schema);
        log.debug("findUsersWithNoUploadToday – schema={}", schema);
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Returns users with at least {@code minMissedDays} missed uploads within the
     * last {@code lookbackDays} days, along with their last reading date and
     * the total missed count within that window.
     */
    public List<Map<String, Object>> findUsersWithMissedDays(String schema,
                                                              int lookbackDays,
                                                              int minMissedDays) {
        validateSchemaName(schema);
        String sql = String.format("""
                SELECT
                  u.id,
                  u.name,
                  u.phone_number,
                  usm.scheme_id,
                  sm.state_scheme_id AS scheme_name,
                  MAX(fr.reading_date) AS last_reading_date,
                  (SELECT COUNT(*)
                   FROM generate_series(
                       CURRENT_DATE - ? * INTERVAL '1 day',
                       CURRENT_DATE - INTERVAL '1 day',
                       INTERVAL '1 day'
                   ) gs(day)
                   WHERE gs.day::DATE NOT IN (
                       SELECT DISTINCT reading_date
                       FROM %s.flow_reading_table
                       WHERE scheme_id = usm.scheme_id AND created_by = u.id
                   )
                  ) AS missed_days
                FROM %s.user_scheme_mapping_table usm
                JOIN %s.user_table u ON u.id = usm.user_id
                LEFT JOIN %s.flow_reading_table fr
                    ON fr.scheme_id = usm.scheme_id AND fr.created_by = u.id
                LEFT JOIN %s.scheme_master_table sm ON sm.id = usm.scheme_id
                WHERE usm.status = 'ACTIVE'
                GROUP BY u.id, u.name, u.phone_number, usm.scheme_id, sm.state_scheme_id
                HAVING (SELECT COUNT(*)
                        FROM generate_series(
                            CURRENT_DATE - ? * INTERVAL '1 day',
                            CURRENT_DATE - INTERVAL '1 day',
                            INTERVAL '1 day'
                        ) gs(day)
                        WHERE gs.day::DATE NOT IN (
                            SELECT DISTINCT reading_date
                            FROM %s.flow_reading_table
                            WHERE scheme_id = usm.scheme_id AND created_by = u.id
                        )
                       ) >= ?
                """,
                schema, schema, schema, schema, schema, schema);

        log.debug("findUsersWithMissedDays – schema={}, lookback={}, min={}", schema, lookbackDays, minMissedDays);
        return jdbcTemplate.queryForList(sql, lookbackDays, lookbackDays, minMissedDays);
    }

    /**
     * Returns the name and phone number of the officer (by {@code userTypeName})
     * mapped to a given scheme, or {@code null} if none found.
     */
    public Map<String, Object> findOfficerByUserType(String schema, Object schemeId, String userTypeName) {
        validateSchemaName(schema);
        String sql = String.format("""
                SELECT u.name, u.phone_number
                FROM %s.user_scheme_mapping_table usm
                JOIN %s.user_table u ON u.id = usm.user_id
                JOIN common_schema.user_type_master_table ut ON ut.id = u.user_type_id
                WHERE usm.scheme_id = ? AND ut.c_name = ? AND usm.status = 'ACTIVE'
                LIMIT 1
                """, schema, schema);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, schemeId, userTypeName);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void validateSchemaName(String schema) {
        if (schema == null || !schema.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schema);
        }
    }
}
