package org.arghyam.jalsoochak.telemetry.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserLanguagePreferenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public void upsert(Integer tenantId, String contactId, String languageValue) {
        String normalizedContactId = normalizeContactId(contactId);
        String sql = """
                INSERT INTO common_schema.user_language_preference
                    (tenant_id, contact_id, language_value, created_at, updated_at)
                VALUES (?, ?, ?, NOW(), NOW())
                ON CONFLICT (tenant_id, contact_id)
                DO UPDATE SET language_value = EXCLUDED.language_value,
                              updated_at = NOW()
                """;
        jdbcTemplate.update(sql, tenantId, normalizedContactId, languageValue);
    }

    public Optional<String> findLanguage(Integer tenantId, String contactId) {
        String normalizedContactId = normalizeContactId(contactId);
        String sql = """
                SELECT language_value
                FROM common_schema.user_language_preference
                WHERE tenant_id = ?
                  AND (
                      contact_id = ?
                      OR regexp_replace(COALESCE(contact_id, ''), '\\D', '', 'g') = ?
                  )
                LIMIT 1
                """;
        List<String> rows = jdbcTemplate.query(
                sql,
                (rs, n) -> rs.getString("language_value"),
                tenantId,
                contactId,
                normalizedContactId
        );
        return rows.stream().findFirst();
    }

    public Optional<Integer> findPreferredTenantIdByContactId(String contactId) {
        String normalizedContactId = normalizeContactId(contactId);
        String sql = """
                SELECT tenant_id
                FROM common_schema.user_language_preference
                WHERE contact_id = ?
                   OR regexp_replace(COALESCE(contact_id, ''), '\\D', '', 'g') = ?
                ORDER BY updated_at DESC NULLS LAST, created_at DESC NULLS LAST
                LIMIT 1
                """;
        List<Integer> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getInt("tenant_id"), contactId, normalizedContactId);
        return rows.stream().findFirst();
    }

    private String normalizeContactId(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }
}
