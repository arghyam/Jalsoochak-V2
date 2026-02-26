package com.example.telemetry.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TelemetryTenantRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final int OPERATOR_LOOKUP_CACHE_SIZE = 10_000;
    private final Map<String, String> phoneToSchemaCache = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > OPERATOR_LOOKUP_CACHE_SIZE;
                }
            }
    );

    public boolean existsSchemeById(String schemaName, Long schemeId) {
        validateSchemaName(schemaName);
        String sql = String.format("SELECT EXISTS (SELECT 1 FROM %s.scheme_master_table WHERE id = ?)", schemaName);
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, schemeId);
        return Boolean.TRUE.equals(exists);
    }

    public Optional<TelemetryOperator> findOperatorById(String schemaName, Long operatorId) {
        validateSchemaName(schemaName);
        String languageColumn = resolveSelectColumn(schemaName, "user_table", "language_id", "NULL::integer AS language_id");
        String sql = String.format("""
                SELECT id, tenant_id, title, email, phone_number, language_id
                FROM %s.user_table
                WHERE id = ?
                LIMIT 1
                """, schemaName);
        sql = sql.replace("language_id", languageColumn);
        List<TelemetryOperator> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TelemetryOperator(
                        toLong(rs.getObject("id")),
                        toInteger(rs.getObject("tenant_id")),
                        rs.getString("title"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        toInteger(rs.getObject("language_id"))
                ), operatorId);
        return rows.stream().findFirst();
    }

    public Optional<TelemetryOperatorWithSchema> findOperatorByPhoneAcrossTenants(String phoneNumber) {
        return findOperatorByPhoneAcrossTenants(phoneNumber, null);
    }

    public Optional<TelemetryOperatorWithSchema> findOperatorByPhoneAcrossTenants(String phoneNumber, Integer preferredTenantId) {
        String normalizedPhone = normalizePhone(phoneNumber);
        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            return Optional.empty();
        }

        if (preferredTenantId != null) {
            Optional<String> preferredSchema = findSchemaByTenantId(preferredTenantId);
            if (preferredSchema.isPresent()) {
                Optional<TelemetryOperator> preferredMatch = findOperatorByPhone(
                        preferredSchema.get(),
                        phoneNumber,
                        normalizedPhone
                );
                if (preferredMatch.isPresent()) {
                    phoneToSchemaCache.put(normalizedPhone, preferredSchema.get());
                    return Optional.of(new TelemetryOperatorWithSchema(preferredSchema.get(), preferredMatch.get()));
                }
            }
        }

        String cachedSchema = phoneToSchemaCache.get(normalizedPhone);
        if (cachedSchema != null) {
            Optional<TelemetryOperator> cachedMatch = findOperatorByPhone(cachedSchema, phoneNumber, normalizedPhone);
            if (cachedMatch.isPresent()) {
                return Optional.of(new TelemetryOperatorWithSchema(cachedSchema, cachedMatch.get()));
            }
            phoneToSchemaCache.remove(normalizedPhone);
        }

        String schemaSql = """
                SELECT nspname
                FROM pg_namespace
                WHERE nspname LIKE 'tenant_%'
                ORDER BY nspname
                """;
        List<String> schemas = jdbcTemplate.query(schemaSql, (rs, n) -> rs.getString("nspname"));
        TelemetryOperatorWithSchema firstMatch = null;
        for (String schemaName : schemas) {
            Optional<TelemetryOperator> operator = findOperatorByPhone(schemaName, phoneNumber, normalizedPhone);
            if (operator.isPresent()) {
                TelemetryOperatorWithSchema match = new TelemetryOperatorWithSchema(schemaName, operator.get());
                if (preferredTenantId != null && preferredTenantId.equals(match.operator().tenantId())) {
                    phoneToSchemaCache.put(normalizedPhone, schemaName);
                    return Optional.of(match);
                }
                if (firstMatch == null) {
                    firstMatch = match;
                }
            }
        }
        if (firstMatch != null) {
            phoneToSchemaCache.put(normalizedPhone, firstMatch.schemaName());
        }
        return Optional.ofNullable(firstMatch);
    }

    public Optional<Long> findFirstSchemeForUser(String schemaName, Long userId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT scheme_id
                FROM %s.user_scheme_mapping_table
                WHERE user_id = ?
                  AND status = 1
                ORDER BY id
                LIMIT 1
                """, schemaName);
        List<Long> rows = jdbcTemplate.query(sql, (rs, n) -> toLong(rs.getObject("scheme_id")), userId);
        return rows.stream().findFirst();
    }

    public Optional<Integer> findUserLanguageId(String schemaName, Long userId) {
        validateSchemaName(schemaName);
        if (!columnExists(schemaName, "user_table", "language_id")) {
            return Optional.empty();
        }
        String sql = String.format("""
                SELECT language_id
                FROM %s.user_table
                WHERE id = ?
                LIMIT 1
                """, schemaName);
        List<Integer> rows = jdbcTemplate.query(sql, (rs, n) -> toInteger(rs.getObject("language_id")), userId);
        return rows.stream().findFirst();
    }

    public void updateUserLanguageId(String schemaName, Long userId, Integer languageId) {
        validateSchemaName(schemaName);
        if (!columnExists(schemaName, "user_table", "language_id")) {
            throw new IllegalStateException("Missing required column " + schemaName + ".user_table.language_id");
        }
        String sql = String.format("""
                UPDATE %s.user_table
                SET language_id = ?, updated_at = NOW()
                WHERE id = ?
                """, schemaName);
        jdbcTemplate.update(sql, languageId, userId);
    }

    public Optional<Integer> findSchemeChannel(String schemaName, Long schemeId) {
        validateSchemaName(schemaName);
        String channelColumn = resolveSelectColumn(schemaName, "scheme_master_table", "channel", "NULL::integer AS channel");
        String sql = String.format("""
                SELECT channel
                FROM %s.scheme_master_table
                WHERE id = ?
                LIMIT 1
                """, schemaName);
        sql = sql.replace("channel", channelColumn);
        List<Integer> rows = jdbcTemplate.query(sql, (rs, n) -> toInteger(rs.getObject("channel")), schemeId);
        return rows.stream().findFirst();
    }

    public void updateSchemeChannel(String schemaName, Long schemeId, Integer channel) {
        validateSchemaName(schemaName);
        if (!columnExists(schemaName, "scheme_master_table", "channel")) {
            throw new IllegalStateException("Missing required column " + schemaName + ".scheme_master_table.channel");
        }
        String sql = String.format("""
                UPDATE %s.scheme_master_table
                SET channel = ?, updated_at = NOW()
                WHERE id = ?
                """, schemaName);
        jdbcTemplate.update(sql, channel, schemeId);
    }

    public boolean isOperatorMappedToScheme(String schemaName, Long operatorId, Long schemeId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT EXISTS (
                    SELECT 1
                    FROM %s.user_scheme_mapping_table
                    WHERE user_id = ?
                      AND scheme_id = ?
                      AND status = 1
                )
                """, schemaName);
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, operatorId, schemeId);
        return Boolean.TRUE.equals(exists);
    }

    public Long createFlowReading(String schemaName,
                                  Long schemeId,
                                  Long operatorId,
                                  LocalDateTime readingAt,
                                  BigDecimal extractedReading,
                                  BigDecimal confirmedReading,
                                  String correlationId,
                                  String imageUrl,
                                  String meterChangeReason) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                INSERT INTO %s.flow_reading_table
                    (scheme_id, reading_at, reading_date, extracted_reading, confirmed_reading,
                     correlation_id, quantity, channel, meter_change_reason, image_url, created_by, created_at, updated_by, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, NULL, ?, ?, ?, NOW(), ?, NOW())
                RETURNING id
                """, schemaName);

        Number id = jdbcTemplate.queryForObject(
                sql,
                Number.class,
                schemeId,
                readingAt,
                LocalDate.from(readingAt),
                extractedReading,
                confirmedReading,
                correlationId,
                meterChangeReason,
                imageUrl != null ? imageUrl : "",
                operatorId,
                operatorId
        );
        return id != null ? id.longValue() : null;
    }

    public Long createMeterChangeReasonRecord(String schemaName,
                                              Long schemeId,
                                              Long operatorId,
                                              LocalDateTime readingAt,
                                              String correlationId,
                                              String reason) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                INSERT INTO %s.flow_reading_table
                    (scheme_id, reading_at, reading_date, extracted_reading, confirmed_reading,
                     correlation_id, quantity, channel, meter_change_reason, image_url, created_by, created_at, updated_by, updated_at)
                VALUES (?, ?, ?, 0, 0, ?, 0, NULL, ?, '', ?, NOW(), ?, NOW())
                RETURNING id
                """, schemaName);

        Number id = jdbcTemplate.queryForObject(
                sql,
                Number.class,
                schemeId,
                readingAt,
                LocalDate.from(readingAt),
                correlationId,
                reason,
                operatorId,
                operatorId
        );
        return id != null ? id.longValue() : null;
    }

    public Optional<TelemetryReadingRecord> findLatestPendingMeterChangeRecord(String schemaName, Long schemeId, Long operatorId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT id, correlation_id, created_by
                FROM %s.flow_reading_table
                WHERE scheme_id = ?
                  AND created_by = ?
                  AND extracted_reading = 0
                  AND confirmed_reading = 0
                  AND meter_change_reason IS NOT NULL
                  AND deleted_at IS NULL
                ORDER BY id DESC
                LIMIT 1
                """, schemaName);
        List<TelemetryReadingRecord> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TelemetryReadingRecord(
                        toLong(rs.getObject("id")),
                        rs.getString("correlation_id"),
                        toLong(rs.getObject("created_by"))
                ), schemeId, operatorId);
        return rows.stream().findFirst();
    }

    public Optional<TelemetryReadingRecord> findPendingMeterChangeRecordByCorrelation(String schemaName,
                                                                                       Long schemeId,
                                                                                       Long operatorId,
                                                                                       String correlationId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT id, correlation_id, created_by
                FROM %s.flow_reading_table
                WHERE scheme_id = ?
                  AND created_by = ?
                  AND correlation_id = ?
                  AND extracted_reading = 0
                  AND confirmed_reading = 0
                  AND meter_change_reason IS NOT NULL
                  AND deleted_at IS NULL
                LIMIT 1
                """, schemaName);
        List<TelemetryReadingRecord> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TelemetryReadingRecord(
                        toLong(rs.getObject("id")),
                        rs.getString("correlation_id"),
                        toLong(rs.getObject("created_by"))
                ), schemeId, operatorId, correlationId);
        return rows.stream().findFirst();
    }

    public String upsertPendingMeterChangeRecord(String schemaName,
                                                 Long schemeId,
                                                 Long operatorId,
                                                 LocalDateTime readingAt,
                                                 String reason) {
        Optional<TelemetryReadingRecord> pending = findLatestPendingMeterChangeRecord(schemaName, schemeId, operatorId);
        if (pending.isPresent()) {
            String sql = String.format("""
                    UPDATE %s.flow_reading_table
                    SET reading_at = ?,
                        reading_date = ?,
                        meter_change_reason = ?,
                        updated_by = ?,
                        updated_at = NOW()
                    WHERE id = ?
                    """, schemaName);
            jdbcTemplate.update(sql, readingAt, LocalDate.from(readingAt), reason, operatorId, pending.get().id());
            cleanupOtherPendingMeterChangeRecords(schemaName, schemeId, operatorId, pending.get().id(), operatorId);
            return pending.get().correlationId();
        }

        String correlationId = "meter-change-" + UUID.randomUUID();
        Long createdId = createMeterChangeReasonRecord(schemaName, schemeId, operatorId, readingAt, correlationId, reason);
        cleanupOtherPendingMeterChangeRecords(schemaName, schemeId, operatorId, createdId, operatorId);
        return correlationId;
    }

    private void cleanupOtherPendingMeterChangeRecords(String schemaName,
                                                       Long schemeId,
                                                       Long operatorId,
                                                       Long keepId,
                                                       Long updatedBy) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                UPDATE %s.flow_reading_table
                SET deleted_at = NOW(),
                    deleted_by = ?,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE scheme_id = ?
                  AND created_by = ?
                  AND extracted_reading = 0
                  AND confirmed_reading = 0
                  AND meter_change_reason IS NOT NULL
                  AND deleted_at IS NULL
                  AND id <> ?
                """, schemaName);
        jdbcTemplate.update(sql, updatedBy, updatedBy, schemeId, operatorId, keepId);
    }

    public void updatePendingMeterChangeReading(String schemaName,
                                                Long readingId,
                                                BigDecimal readingValue,
                                                Long updatedBy) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                UPDATE %s.flow_reading_table
                SET extracted_reading = ?,
                    confirmed_reading = ?,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                """, schemaName);
        jdbcTemplate.update(sql, readingValue, readingValue, updatedBy, readingId);
    }

    public Optional<BigDecimal> findLastConfirmedReading(String schemaName, Long schemeId, Long excludeReadingId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT confirmed_reading
                FROM %s.flow_reading_table
                WHERE scheme_id = ?
                  AND id <> ?
                  AND confirmed_reading > 0
                ORDER BY reading_at DESC
                LIMIT 1
                """, schemaName);
        List<BigDecimal> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getBigDecimal("confirmed_reading"), schemeId, excludeReadingId);
        return rows.stream().findFirst();
    }

    public Optional<TelemetryReadingRecord> findReadingByCorrelationId(String schemaName, String correlationId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT id, correlation_id, created_by
                FROM %s.flow_reading_table
                WHERE correlation_id = ?
                LIMIT 1
                """, schemaName);
        List<TelemetryReadingRecord> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TelemetryReadingRecord(
                        toLong(rs.getObject("id")),
                        rs.getString("correlation_id"),
                        toLong(rs.getObject("created_by"))
                ), correlationId);
        return rows.stream().findFirst();
    }

    public void updateConfirmedReading(String schemaName, Long readingId, BigDecimal confirmedReading, Long updatedBy) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                UPDATE %s.flow_reading_table
                SET confirmed_reading = ?, updated_by = ?, updated_at = NOW()
                WHERE id = ?
                """, schemaName);
        jdbcTemplate.update(sql, confirmedReading, updatedBy, readingId);
    }

    private Optional<TelemetryOperator> findOperatorByPhone(String schemaName, String rawPhoneNumber, String normalizedPhone) {
        validateSchemaName(schemaName);
        String languageColumn = resolveSelectColumn(schemaName, "user_table", "language_id", "NULL::integer AS language_id");
        String sql = String.format("""
                SELECT id, tenant_id, title, email, phone_number, language_id
                FROM %s.user_table
                WHERE phone_number = ?
                   OR regexp_replace(COALESCE(phone_number, ''), '\\\\D', '', 'g') = ?
                LIMIT 1
                """, schemaName);
        sql = sql.replace("language_id", languageColumn);
        List<TelemetryOperator> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TelemetryOperator(
                        toLong(rs.getObject("id")),
                        toInteger(rs.getObject("tenant_id")),
                        rs.getString("title"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        toInteger(rs.getObject("language_id"))
                ), rawPhoneNumber, normalizedPhone);
        return rows.stream().findFirst();
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    private boolean columnExists(String schemaName, String tableName, String columnName) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = ?
                      AND table_name = ?
                      AND column_name = ?
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, schemaName, tableName, columnName);
        return Boolean.TRUE.equals(exists);
    }

    private String resolveSelectColumn(String schemaName, String tableName, String columnName, String fallbackExpression) {
        return columnExists(schemaName, tableName, columnName) ? columnName : fallbackExpression;
    }

    private Optional<String> findSchemaByTenantId(Integer tenantId) {
        String sql = """
                SELECT state_code
                FROM common_schema.tenant_master_table
                WHERE id = ?
                LIMIT 1
                """;
        List<String> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getString("state_code"), tenantId);
        return rows.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> "tenant_" + code.trim().toLowerCase())
                .findFirst();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Expected numeric DB value, got: " + value.getClass().getName());
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Expected numeric DB value, got: " + value.getClass().getName());
    }
}
