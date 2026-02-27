package org.arghyam.jalsoochak.telemetry.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TenantConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<Integer> findTenantIdByStateCode(String tenantCode) {
        String sql = """
                SELECT id
                FROM common_schema.tenant_master_table
                WHERE LOWER(state_code) = LOWER(?)
                LIMIT 1
                """;
        List<Integer> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getInt("id"), tenantCode);
        return rows.stream().findFirst();
    }

    public Optional<String> findLanguageSelectionPrompt(Integer tenantId) {
        String sql = """
                SELECT config_value
                FROM common_schema.tenant_config_master_table
                WHERE tenant_id = ?
                  AND config_key = 'language_selection_prompt'
                LIMIT 1
                """;
        List<String> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getString("config_value"), tenantId);
        return rows.stream().findFirst();
    }

    public List<String> findLanguageOptions(Integer tenantId) {
        String sql = """
                SELECT config_value
                FROM common_schema.tenant_config_master_table
                WHERE tenant_id = ?
                  AND config_key ~ '^language_[0-9]+$'
                ORDER BY split_part(config_key, '_', 2)::int
                """;
        return jdbcTemplate.query(sql, (rs, n) -> rs.getString("config_value"), tenantId);
    }

    public Optional<String> findConfigValue(Integer tenantId, String configKey) {
        String sql = """
                SELECT config_value
                FROM common_schema.tenant_config_master_table
                WHERE tenant_id = ?
                  AND config_key = ?
                LIMIT 1
                """;
        List<String> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getString("config_value"), tenantId, configKey);
        return rows.stream().findFirst();
    }

    public Optional<String> findChannelSelectionPrompt(Integer tenantId, String languageKey) {
        String langSpecificKey = "channel_selection_prompt_" + languageKey;
        return findConfigValue(tenantId, langSpecificKey)
                .or(() -> findConfigValue(tenantId, "channel_selection_prompt"));
    }

    public List<String> findChannelOptions(Integer tenantId, String languageKey) {
        String localizedSql = """
                SELECT config_value
                FROM common_schema.tenant_config_master_table
                WHERE tenant_id = ?
                  AND config_key ~ '^channel_[0-9]+_%s$'
                ORDER BY regexp_replace(config_key, '^channel_([0-9]+)_%s$', '\\1')::int
                """.formatted(languageKey, languageKey);
        List<String> localized = jdbcTemplate.query(localizedSql, (rs, n) -> rs.getString("config_value"), tenantId);
        if (!localized.isEmpty()) {
            return localized;
        }

        String genericSql = """
                SELECT config_value
                FROM common_schema.tenant_config_master_table
                WHERE tenant_id = ?
                  AND config_key ~ '^channel_[0-9]+$'
                ORDER BY split_part(config_key, '_', 2)::int
                """;
        return jdbcTemplate.query(genericSql, (rs, n) -> rs.getString("config_value"), tenantId);
    }

    public Optional<String> findItemSelectionPrompt(Integer tenantId, String languageKey) {
        String langSpecificKey = "item_selection_prompt_" + languageKey;
        return findConfigValue(tenantId, langSpecificKey)
                .or(() -> findConfigValue(tenantId, "item_selection_prompt"));
    }

    public List<String> findItemOptions(Integer tenantId, String languageKey) {
        String localizedSql = """
                SELECT config_value
                FROM common_schema.tenant_config_master_table
                WHERE tenant_id = ?
                  AND config_key ~ '^item_[0-9]+_%s$'
                ORDER BY regexp_replace(config_key, '^item_([0-9]+)_%s$', '\\1')::int
                """.formatted(languageKey, languageKey);
        List<String> localized = jdbcTemplate.query(localizedSql, (rs, n) -> rs.getString("config_value"), tenantId);
        if (!localized.isEmpty()) {
            return localized;
        }

        String genericSql = """
                SELECT config_value
                FROM common_schema.tenant_config_master_table
                WHERE tenant_id = ?
                  AND config_key ~ '^item_[0-9]+$'
                ORDER BY split_part(config_key, '_', 2)::int
                """;
        return jdbcTemplate.query(genericSql, (rs, n) -> rs.getString("config_value"), tenantId);
    }

    public Optional<String> findMeterChangePrompt(Integer tenantId, String languageKey) {
        String langSpecificKey = "meter_change_prompt_" + languageKey;
        return findConfigValue(tenantId, langSpecificKey)
                .or(() -> findConfigValue(tenantId, "meter_change_prompt"));
    }

    public List<String> findMeterChangeReasons(Integer tenantId, String languageKey) {
        String localizedSql = """
                SELECT config_value
                FROM common_schema.tenant_config_master_table
                WHERE tenant_id = ?
                  AND config_key ~ '^meter_change_reason_[0-9]+_%s$'
                ORDER BY regexp_replace(config_key, '^meter_change_reason_([0-9]+)_%s$', '\\1')::int
                """.formatted(languageKey, languageKey);
        List<String> localized = jdbcTemplate.query(localizedSql, (rs, n) -> rs.getString("config_value"), tenantId);
        if (!localized.isEmpty()) {
            return localized;
        }

        String genericSql = """
                SELECT config_value
                FROM common_schema.tenant_config_master_table
                WHERE tenant_id = ?
                  AND config_key ~ '^meter_change_reason_[0-9]+$'
                ORDER BY regexp_replace(config_key, '^meter_change_reason_([0-9]+)$', '\\1')::int
                """;
        return jdbcTemplate.query(genericSql, (rs, n) -> rs.getString("config_value"), tenantId);
    }

    public Optional<String> findTakeMeterReadingPrompt(Integer tenantId, String languageKey) {
        String langSpecificKey = "take_meter_reading_prompt_" + languageKey;
        return findConfigValue(tenantId, langSpecificKey)
                .or(() -> findConfigValue(tenantId, "take_meter_reading_prompt"));
    }

    public Optional<String> findManualReadingConfirmationTemplate(Integer tenantId, String languageKey) {
        String langSpecificKey = "manual_reading_confirmation_template_" + languageKey;
        return findConfigValue(tenantId, langSpecificKey)
                .or(() -> findConfigValue(tenantId, "manual_reading_confirmation_template"));
    }

    public Optional<String> findMeterChangeConfirmationTemplate(Integer tenantId, String languageKey) {
        String langSpecificKey = "meter_change_confirmation_template_" + languageKey;
        return findConfigValue(tenantId, langSpecificKey)
                .or(() -> findConfigValue(tenantId, "meter_change_confirmation_template"));
    }
}
