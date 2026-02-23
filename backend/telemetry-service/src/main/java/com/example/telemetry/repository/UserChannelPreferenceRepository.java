package com.example.telemetry.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserChannelPreferenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public void upsert(Integer tenantId, String contactId, String channelValue) {
        String normalizedContactId = normalizeContactId(contactId);
        String sql = """
                INSERT INTO common_schema.user_channel_preference
                    (tenant_id, contact_id, channel_value, created_at, updated_at)
                VALUES (?, ?, ?, NOW(), NOW())
                ON CONFLICT (tenant_id, contact_id)
                DO UPDATE SET channel_value = EXCLUDED.channel_value,
                              updated_at = NOW()
                """;
        jdbcTemplate.update(sql, tenantId, normalizedContactId, channelValue);
    }

    private String normalizeContactId(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }
}
