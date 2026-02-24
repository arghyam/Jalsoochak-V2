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
     * Returns OPERATOR users who have an active scheme mapping but no flow reading for today.
     * These are candidates for a nudge notification.
     */
    public List<Map<String, Object>> findUsersWithNoUploadToday(String schema) {
        validateSchemaName(schema);
        String sql = String.format("""
                SELECT u.id, u.name, u.phone_number, usm.scheme_id
                FROM %s.user_scheme_mapping_table usm
                JOIN %s.user_table u ON u.id = usm.user_id
                JOIN common_schema.user_type_master_table ut ON ut.id = u.user_type_id
                LEFT JOIN %s.flow_reading_table fr
                    ON fr.scheme_id = usm.scheme_id
                    AND fr.created_by = u.id
                    AND fr.reading_date = CURRENT_DATE
                WHERE usm.status = 'ACTIVE'
                  AND ut.c_name = 'OPERATOR'
                  AND fr.id IS NULL
                """, schema, schema, schema);
        log.debug("findUsersWithNoUploadToday – schema={}", schema);
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Returns OPERATOR users who have missed at least {@code minMissedDays} consecutive
     * days of uploads, plus any operators who have NEVER uploaded (returned with a
     * {@code null} value for {@code days_since_last_upload}).
     *
     * <p>{@code days_since_last_upload} is the calendar-day gap between the operator's
     * most recent reading and today: {@code CURRENT_DATE - MAX(reading_date)}.
     * It is {@code null} when the operator has no readings at all.</p>
     *
     * <ul>
     *   <li>Last upload today → 0 (up to date, excluded by threshold)</li>
     *   <li>Last upload yesterday → 1 day</li>
     *   <li>Last upload 7 days ago → 7 days → level-2 escalation</li>
     *   <li>Never uploaded → NULL → always escalated (level-2)</li>
     * </ul>
     */
    public List<Map<String, Object>> findUsersWithMissedDays(String schema, int minMissedDays) {
        validateSchemaName(schema);
        String sql = String.format("""
                SELECT id, name, phone_number, scheme_id, scheme_name,
                       last_reading_date, days_since_last_upload
                FROM (
                    SELECT
                      u.id,
                      u.name,
                      u.phone_number,
                      usm.scheme_id,
                      sm.state_scheme_id AS scheme_name,
                      MAX(fr.reading_date) AS last_reading_date,
                      CASE
                        WHEN MAX(fr.reading_date) IS NULL THEN NULL
                        ELSE CURRENT_DATE - MAX(fr.reading_date)
                      END AS days_since_last_upload
                    FROM %s.user_scheme_mapping_table usm
                    JOIN %s.user_table u ON u.id = usm.user_id
                    JOIN common_schema.user_type_master_table ut ON ut.id = u.user_type_id
                    LEFT JOIN %s.flow_reading_table fr
                        ON fr.scheme_id = usm.scheme_id AND fr.created_by = u.id
                    LEFT JOIN %s.scheme_master_table sm ON sm.id = usm.scheme_id
                    WHERE usm.status = 'ACTIVE'
                      AND ut.c_name = 'OPERATOR'
                    GROUP BY u.id, u.name, u.phone_number, usm.scheme_id, sm.state_scheme_id
                ) sub
                WHERE days_since_last_upload IS NULL
                   OR days_since_last_upload >= ?
                """, schema, schema, schema, schema);

        log.debug("findUsersWithMissedDays – schema={}, minMissedDays={}", schema, minMissedDays);
        return jdbcTemplate.queryForList(sql, minMissedDays);
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
