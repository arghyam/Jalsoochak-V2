package com.example.tenant.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TenantConfigService} verifying that escalation
 * threshold values are correctly read from the database, with application-level
 * defaults as fallback.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class TenantConfigServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withInitScript("sql/test-schema.sql");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanConfig() {
        jdbcTemplate.execute("DELETE FROM common_schema.tenant_config_master_table");
    }

    @Test
    void getLevel1ThresholdDays_returnsDefault_whenKeyAbsent() {
        // No rows in config table
        assertThat(tenantConfigService.getLevel1ThresholdDays()).isEqualTo(3);
    }

    @Test
    void getLevel2ThresholdDays_returnsDefault_whenKeyAbsent() {
        assertThat(tenantConfigService.getLevel2ThresholdDays()).isEqualTo(7);
    }

    @Test
    void getLevel1OfficerUserType_returnsDefault_whenKeyAbsent() {
        assertThat(tenantConfigService.getLevel1OfficerUserType()).isEqualTo("SECTION_OFFICER");
    }

    @Test
    void getLevel2OfficerUserType_returnsDefault_whenKeyAbsent() {
        assertThat(tenantConfigService.getLevel2OfficerUserType()).isEqualTo("DISTRICT_OFFICER");
    }

    @Test
    void getLevel1ThresholdDays_returnsValueFromDatabase_whenKeyPresent() {
        insertConfig("escalation.level1.threshold.days", "5");

        assertThat(tenantConfigService.getLevel1ThresholdDays()).isEqualTo(5);
    }

    @Test
    void getLevel2ThresholdDays_returnsValueFromDatabase_whenKeyPresent() {
        insertConfig("escalation.level2.threshold.days", "10");

        assertThat(tenantConfigService.getLevel2ThresholdDays()).isEqualTo(10);
    }

    @Test
    void getLevel1OfficerUserType_returnsValueFromDatabase_whenKeyPresent() {
        insertConfig("escalation.level1.officer.user_type", "JUNIOR_ENGINEER");

        assertThat(tenantConfigService.getLevel1OfficerUserType()).isEqualTo("JUNIOR_ENGINEER");
    }

    @Test
    void getLevel2OfficerUserType_returnsValueFromDatabase_whenKeyPresent() {
        insertConfig("escalation.level2.officer.user_type", "EXECUTIVE_ENGINEER");

        assertThat(tenantConfigService.getLevel2OfficerUserType()).isEqualTo("EXECUTIVE_ENGINEER");
    }

    @Test
    void getLevel1ThresholdDays_returnsDefault_whenValueIsNotANumber() {
        insertConfig("escalation.level1.threshold.days", "not-a-number");

        // Falls back to application default (3)
        assertThat(tenantConfigService.getLevel1ThresholdDays()).isEqualTo(3);
    }

    @Test
    void getLevel2ThresholdDays_returnsDefault_whenValueIsEmpty() {
        insertConfig("escalation.level2.threshold.days", "");

        assertThat(tenantConfigService.getLevel2ThresholdDays()).isEqualTo(7);
    }

    // ────────────────────────────── helpers ────────────────────────────────────

    private void insertConfig(String key, String value) {
        // tenant_id=1 matches the seeded tenant in test-schema.sql
        jdbcTemplate.update(
                "INSERT INTO common_schema.tenant_config_master_table (tenant_id, config_key, config_value) VALUES (1, ?, ?)",
                key, value);
    }
}
