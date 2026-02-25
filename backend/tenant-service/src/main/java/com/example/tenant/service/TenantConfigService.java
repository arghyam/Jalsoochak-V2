package com.example.tenant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Reads per-tenant configuration from {@code common_schema.tenant_config_master_table}.
 * Falls back to application-level defaults if the table is absent or the key is not found.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantConfigService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${escalation.level1.threshold.days:3}")
    private int defaultLevel1ThresholdDays;

    @Value("${escalation.level2.threshold.days:7}")
    private int defaultLevel2ThresholdDays;

    @Value("${escalation.level1.officer.user_type:SECTION_OFFICER}")
    private String defaultLevel1OfficerUserType;

    @Value("${escalation.level2.officer.user_type:DISTRICT_OFFICER}")
    private String defaultLevel2OfficerUserType;

    public int getLevel1ThresholdDays() {
        return getIntConfig("escalation.level1.threshold.days", defaultLevel1ThresholdDays);
    }

    public int getLevel2ThresholdDays() {
        return getIntConfig("escalation.level2.threshold.days", defaultLevel2ThresholdDays);
    }

    public String getLevel1OfficerUserType() {
        return getStringConfig("escalation.level1.officer.user_type", defaultLevel1OfficerUserType);
    }

    public String getLevel2OfficerUserType() {
        return getStringConfig("escalation.level2.officer.user_type", defaultLevel2OfficerUserType);
    }

    private int getIntConfig(String key, int defaultValue) {
        String value = getStringConfig(key, null);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer config for key '{}': '{}', using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    private String getStringConfig(String key, String defaultValue) {
        try {
            String sql = "SELECT config_value FROM common_schema.tenant_config_master_table WHERE config_key = ? LIMIT 1";
            return jdbcTemplate.queryForObject(sql, String.class, key);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Config key '{}' not found in tenant_config_master_table, using default", key);
            return defaultValue;
        } catch (Exception e) {
            log.warn("Could not read config key '{}' from tenant_config_master_table: {}, using default",
                    key, e.getMessage());
            return defaultValue;
        }
    }
}
