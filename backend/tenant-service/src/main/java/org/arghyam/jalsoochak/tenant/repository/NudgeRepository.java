package org.arghyam.jalsoochak.tenant.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
     * Streams OPERATOR users who have an active scheme mapping but no flow reading for today,
     * calling {@code consumer} once per row. Returns the total row count.
     *
     * <p>Uses a server-side cursor (fetchSize=500) to avoid materialising the full result set
     * into heap, preventing OOM on large tenants.</p>
     */
    @Transactional(readOnly = true)
    public int streamUsersWithNoUploadToday(String schema, Consumer<Map<String, Object>> consumer) {
        validateSchemaName(schema);
        String sql = String.format("""
                SELECT u.id as user_id, u.title as name, u.phone_number, u.language_id,
                       u.whatsapp_connection_id, usm.scheme_id
                FROM %s.user_scheme_mapping_table usm
                JOIN %s.user_table u ON u.id = usm.user_id
                JOIN common_schema.user_type_master_table ut ON ut.id = u.user_type
                LEFT JOIN %s.flow_reading_table fr
                    ON fr.scheme_id = usm.scheme_id
                    AND fr.created_by = u.id
                    AND fr.reading_date = CURRENT_DATE
                WHERE usm.status = 1
                  AND UPPER(ut.c_name) = 'PUMP_OPERATOR'
                  AND fr.id IS NULL
                """, schema, schema, schema);
        log.debug("streamUsersWithNoUploadToday – schema={}", schema);
        int[] count = {0};
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setFetchSize(500);
            return ps;
        }, rs -> {
            Map<String, Object> row = new HashMap<>(8);
            row.put("user_id", rs.getObject("user_id"));
            row.put("name", rs.getString("name"));
            row.put("phone_number", rs.getString("phone_number"));
            row.put("language_id", rs.getObject("language_id"));
            row.put("whatsapp_connection_id", rs.getObject("whatsapp_connection_id"));
            row.put("scheme_id", rs.getObject("scheme_id"));
            consumer.accept(row);
            count[0]++;
        });
        return count[0];
    }

    /**
     * Streams OPERATOR users who have missed at least {@code minMissedDays} consecutive days
     * of uploads, plus any operators who have NEVER uploaded ({@code days_since_last_upload}
     * is {@code null}). Calls {@code consumer} once per row. Returns the total row count.
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
    @Transactional(readOnly = true)
    public int streamUsersWithMissedDays(String schema, int minMissedDays,
                                         Consumer<Map<String, Object>> consumer) {
        validateSchemaName(schema);
        String sql = String.format("""
                SELECT user_id, name, phone_number, language_id, whatsapp_connection_id,
                       scheme_id, scheme_name, last_reading_date, days_since_last_upload
                FROM (
                    SELECT
                      u.id AS user_id,
                      u.title AS name,
                      u.phone_number,
                      u.language_id,
                      u.whatsapp_connection_id,
                      usm.scheme_id,
                      sm.state_scheme_id AS scheme_name,
                      MAX(fr.reading_date) AS last_reading_date,
                      CASE
                        WHEN MAX(fr.reading_date) IS NULL THEN NULL
                        ELSE CURRENT_DATE - MAX(fr.reading_date)
                      END AS days_since_last_upload
                    FROM %s.user_scheme_mapping_table usm
                    JOIN %s.user_table u ON u.id = usm.user_id
                    JOIN common_schema.user_type_master_table ut ON ut.id = u.user_type
                    LEFT JOIN %s.flow_reading_table fr
                        ON fr.scheme_id = usm.scheme_id AND fr.created_by = u.id
                    LEFT JOIN %s.scheme_master_table sm ON sm.id = usm.scheme_id
                    WHERE usm.status = 1
                      AND UPPER(ut.c_name) = 'PUMP_OPERATOR'
                    GROUP BY u.id, u.title, u.phone_number, u.language_id, u.whatsapp_connection_id,
                             usm.scheme_id, sm.state_scheme_id
                ) sub
                WHERE days_since_last_upload IS NULL
                   OR days_since_last_upload >= ?
                """, schema, schema, schema, schema);
        log.debug("streamUsersWithMissedDays – schema={}, minMissedDays={}", schema, minMissedDays);
        int[] count = {0};
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, minMissedDays);
            ps.setFetchSize(500);
            return ps;
        }, rs -> {
            Map<String, Object> row = new HashMap<>(10);
            row.put("user_id", rs.getObject("user_id"));
            row.put("name", rs.getString("name"));
            row.put("phone_number", rs.getString("phone_number"));
            row.put("language_id", rs.getObject("language_id"));
            row.put("whatsapp_connection_id", rs.getObject("whatsapp_connection_id"));
            row.put("scheme_id", rs.getObject("scheme_id"));
            row.put("scheme_name", rs.getString("scheme_name"));
            row.put("last_reading_date", rs.getObject("last_reading_date"));
            row.put("days_since_last_upload", rs.getObject("days_since_last_upload"));
            consumer.accept(row);
            count[0]++;
        });
        return count[0];
    }

    /**
     * Returns the name and phone number of the officer (by {@code userTypeName})
     * mapped to a given scheme, or {@code null} if none found.
     */
    public Map<String, Object> findOfficerByUserType(String schema, Object schemeId, String userTypeName) {
        validateSchemaName(schema);
        String sql = String.format("""
                SELECT u.id as user_id, u.title as name, u.phone_number, u.language_id,
                       u.whatsapp_connection_id
                FROM %s.user_scheme_mapping_table usm
                JOIN %s.user_table u ON u.id = usm.user_id
                JOIN common_schema.user_type_master_table ut ON ut.id = u.user_type
                WHERE usm.scheme_id = ? AND UPPER(ut.c_name) = UPPER(?) AND usm.status = 1
                LIMIT 1
                """, schema, schema);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, schemeId, userTypeName);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * Persists the Glific contact ID for the given user.
     * Called by the Kafka consumer when a {@code WHATSAPP_CONTACT_REGISTERED} event arrives.
     */
    public int updateWhatsAppConnectionId(String schema, long userId, long contactId) {
        validateSchemaName(schema);
        return jdbcTemplate.update(
                "UPDATE " + schema + ".user_table SET whatsapp_connection_id = ? WHERE id = ?",
                contactId, userId);
    }

    private void validateSchemaName(String schema) {
        if (schema == null || !schema.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schema);
        }
    }
}
