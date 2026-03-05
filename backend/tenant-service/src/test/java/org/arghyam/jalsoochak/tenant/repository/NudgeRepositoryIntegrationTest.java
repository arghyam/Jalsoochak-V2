package org.arghyam.jalsoochak.tenant.repository;

import org.arghyam.jalsoochak.tenant.service.TenantSchedulerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link NudgeRepository} SQL queries against a real
 * PostgreSQL instance via Testcontainers.
 *
 * <p>Verifies the three core queries:
 * <ul>
 *   <li>{@code findUsersWithNoUploadToday} – nudge candidates</li>
 *   <li>{@code findUsersWithMissedDays} – escalation candidates</li>
 *   <li>{@code findOfficerByUserType} – officer lookup for escalation</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class NudgeRepositoryIntegrationTest {

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

    // Suppress real Kafka connections – these tests only exercise database logic
    @MockBean
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    /** Suppress @PostConstruct scheduling – not under test here. */
    @MockBean
    private TenantSchedulerManager tenantSchedulerManager;

    @Autowired
    private NudgeRepository nudgeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int operatorTypeId;
    private int sectionOfficerTypeId;
    private int districtOfficerTypeId;
    private int schemeId;

    @BeforeEach
    void setUp() {
        operatorTypeId = jdbcTemplate.queryForObject(
                "SELECT id FROM common_schema.user_type_master_table WHERE c_name = 'OPERATOR'",
                Integer.class);
        sectionOfficerTypeId = jdbcTemplate.queryForObject(
                "SELECT id FROM common_schema.user_type_master_table WHERE c_name = 'SECTION_OFFICER'",
                Integer.class);
        districtOfficerTypeId = jdbcTemplate.queryForObject(
                "SELECT id FROM common_schema.user_type_master_table WHERE c_name = 'DISTRICT_OFFICER'",
                Integer.class);

        schemeId = jdbcTemplate.queryForObject(
                "INSERT INTO tenant_test.scheme_master_table (state_scheme_id) VALUES ('S-001') RETURNING id",
                Integer.class);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM tenant_test.flow_reading_table");
        jdbcTemplate.execute("DELETE FROM tenant_test.user_scheme_mapping_table");
        jdbcTemplate.execute("DELETE FROM tenant_test.user_table");
        jdbcTemplate.execute("DELETE FROM tenant_test.scheme_master_table");
    }

    // ───────────────────────────── findUsersWithNoUploadToday ─────────────────

    @Test
    void findUsersWithNoUploadToday_returnsOperator_whenNoReadingSubmittedToday() {
        int opId = insertUser("Op One", "911111111111", operatorTypeId);
        insertSchemeMapping(opId, schemeId, 1);

        List<Map<String, Object>> result = nudgeRepository.findUsersWithNoUploadToday("tenant_test");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("phone_number")).isEqualTo("911111111111");
        assertThat(result.get(0).get("name")).isEqualTo("Op One");
    }

    @Test
    void findUsersWithNoUploadToday_excludesOperator_whenReadingSubmittedToday() {
        int opId = insertUser("Op Two", "912222222222", operatorTypeId);
        insertSchemeMapping(opId, schemeId, 1);
        insertFlowReading(schemeId, opId, LocalDate.now());

        List<Map<String, Object>> result = nudgeRepository.findUsersWithNoUploadToday("tenant_test");

        assertThat(result).isEmpty();
    }

    @Test
    void findUsersWithNoUploadToday_excludesNonOperatorUserTypes() {
        int officerId = insertUser("SO Officer", "913333333333", sectionOfficerTypeId);
        insertSchemeMapping(officerId, schemeId, 1);

        List<Map<String, Object>> result = nudgeRepository.findUsersWithNoUploadToday("tenant_test");

        assertThat(result).isEmpty();
    }

    @Test
    void findUsersWithNoUploadToday_excludesOperator_withInactiveSchemeMappingStatus() {
        int opId = insertUser("Op Inactive", "914444444444", operatorTypeId);
        insertSchemeMapping(opId, schemeId, 0); // status=0 means inactive

        List<Map<String, Object>> result = nudgeRepository.findUsersWithNoUploadToday("tenant_test");

        assertThat(result).isEmpty();
    }

    @Test
    void findUsersWithNoUploadToday_includesOperator_whenReadingFromYesterdayOnly() {
        int opId = insertUser("Op Yesterday", "915555555555", operatorTypeId);
        insertSchemeMapping(opId, schemeId, 1);
        insertFlowReading(schemeId, opId, LocalDate.now().minusDays(1));

        List<Map<String, Object>> result = nudgeRepository.findUsersWithNoUploadToday("tenant_test");

        assertThat(result).hasSize(1);
    }

    @Test
    void findUsersWithNoUploadToday_returnsLanguageId() {
        int opId = insertUserWithLanguage("Op Lang", "916601010101", operatorTypeId, 2);
        insertSchemeMapping(opId, schemeId, 1);

        List<Map<String, Object>> result = nudgeRepository.findUsersWithNoUploadToday("tenant_test");

        assertThat(result).hasSize(1);
        assertThat(((Number) result.get(0).get("language_id")).intValue()).isEqualTo(2);
    }

    // ─────────────────────────── findUsersWithMissedDays ──────────────────────

    @Test
    void findUsersWithMissedDays_returnsOperator_whoHasNeverUploaded() {
        int opId = insertUser("Op Never", "916666666666", operatorTypeId);
        insertSchemeMapping(opId, schemeId, 1);
        // no flow_reading rows → never uploaded

        List<Map<String, Object>> result = nudgeRepository.findUsersWithMissedDays("tenant_test", 3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("phone_number")).isEqualTo("916666666666");
        assertThat(result.get(0).get("days_since_last_upload")).isNull(); // NULL = never uploaded
    }

    @Test
    void findUsersWithMissedDays_returnsOperator_whenDaysExceedThreshold() {
        int opId = insertUser("Op Missed", "917777777777", operatorTypeId);
        insertSchemeMapping(opId, schemeId, 1);
        insertFlowReading(schemeId, opId, LocalDate.now().minusDays(5));

        List<Map<String, Object>> result = nudgeRepository.findUsersWithMissedDays("tenant_test", 3);

        assertThat(result).hasSize(1);
        Number daysMissed = (Number) result.get(0).get("days_since_last_upload");
        assertThat(daysMissed.intValue()).isEqualTo(5);
    }

    @Test
    void findUsersWithMissedDays_excludesOperator_whenDaysBelowThreshold() {
        int opId = insertUser("Op Recent", "918888888888", operatorTypeId);
        insertSchemeMapping(opId, schemeId, 1);
        insertFlowReading(schemeId, opId, LocalDate.now().minusDays(2));

        List<Map<String, Object>> result = nudgeRepository.findUsersWithMissedDays("tenant_test", 3);

        assertThat(result).isEmpty();
    }

    @Test
    void findUsersWithMissedDays_returnsOperatorAtExactThreshold() {
        int opId = insertUser("Op Exact", "919191919191", operatorTypeId);
        insertSchemeMapping(opId, schemeId, 1);
        insertFlowReading(schemeId, opId, LocalDate.now().minusDays(3));

        List<Map<String, Object>> result = nudgeRepository.findUsersWithMissedDays("tenant_test", 3);

        assertThat(result).hasSize(1);
        assertThat(((Number) result.get(0).get("days_since_last_upload")).intValue()).isEqualTo(3);
    }

    @Test
    void findUsersWithMissedDays_includesSchemeName() {
        jdbcTemplate.update("UPDATE tenant_test.scheme_master_table SET state_scheme_id = 'MY-SCHEME' WHERE id = ?", schemeId);
        int opId = insertUser("Op Scheme", "917070707070", operatorTypeId);
        insertSchemeMapping(opId, schemeId, 1);

        List<Map<String, Object>> result = nudgeRepository.findUsersWithMissedDays("tenant_test", 0);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).get("scheme_name")).isEqualTo("MY-SCHEME");
    }

    // ─────────────────────────── findOfficerByUserType ────────────────────────

    @Test
    void findOfficerByUserType_returnsOfficer_whenMappedToScheme() {
        int officerId = insertUser("SO Name", "919999999999", sectionOfficerTypeId);
        insertSchemeMapping(officerId, schemeId, 1);

        Map<String, Object> result = nudgeRepository.findOfficerByUserType(
                "tenant_test", schemeId, "SECTION_OFFICER");

        assertThat(result).isNotNull();
        assertThat(result.get("name")).isEqualTo("SO Name");
        assertThat(result.get("phone_number")).isEqualTo("919999999999");
    }

    @Test
    void findOfficerByUserType_returnsNull_whenNoOfficerMapped() {
        Map<String, Object> result = nudgeRepository.findOfficerByUserType(
                "tenant_test", schemeId, "DISTRICT_OFFICER");

        assertThat(result).isNull();
    }

    @Test
    void findOfficerByUserType_returnsNull_whenOfficerHasInactiveMapping() {
        int officerId = insertUser("DO Inactive", "910101010101", districtOfficerTypeId);
        insertSchemeMapping(officerId, schemeId, 0); // inactive mapping

        Map<String, Object> result = nudgeRepository.findOfficerByUserType(
                "tenant_test", schemeId, "DISTRICT_OFFICER");

        assertThat(result).isNull();
    }

    @Test
    void findOfficerByUserType_returnsLanguageId_ofOfficer() {
        int officerId = insertUserWithLanguage("SO Lang", "911122334455", sectionOfficerTypeId, 3);
        insertSchemeMapping(officerId, schemeId, 1);

        Map<String, Object> result = nudgeRepository.findOfficerByUserType(
                "tenant_test", schemeId, "SECTION_OFFICER");

        assertThat(((Number) result.get("language_id")).intValue()).isEqualTo(3);
    }

    // ──────────────────────────── schema validation ────────────────────────────

    @Test
    void findUsersWithNoUploadToday_rejectsInvalidSchemaName() {
        assertThatThrownBy(() -> nudgeRepository.findUsersWithNoUploadToday("invalid-schema!"))
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid schema name");
    }

    @Test
    void findUsersWithMissedDays_rejectsSqlInjectionAttempt() {
        assertThatThrownBy(() ->
                nudgeRepository.findUsersWithMissedDays("'; DROP TABLE users; --", 3))
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid schema name");
    }

    @Test
    void findOfficerByUserType_rejectsInvalidSchemaName() {
        assertThatThrownBy(() ->
                nudgeRepository.findOfficerByUserType("UPPER_CASE", 1, "OPERATOR"))
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    // ────────────────────────────── helpers ────────────────────────────────────

    private int insertUser(String name, String phone, int userTypeId) {
        return insertUserWithLanguage(name, phone, userTypeId, 0);
    }

    private int insertUserWithLanguage(String name, String phone, int userTypeId, int languageId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO tenant_test.user_table (title, phone_number, user_type, language_id, email) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING id",
                Integer.class, name, phone, userTypeId, languageId, phone + "@test.com");
    }

    private void insertSchemeMapping(int userId, int schemeId, int status) {
        jdbcTemplate.update(
                "INSERT INTO tenant_test.user_scheme_mapping_table (user_id, scheme_id, status) VALUES (?, ?, ?)",
                userId, schemeId, status);
    }

    private void insertFlowReading(int schemeId, int createdBy, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO tenant_test.flow_reading_table " +
                "(scheme_id, reading_date, created_by, updated_by) VALUES (?, ?, ?, ?)",
                schemeId, date, createdBy, createdBy);
    }
}
