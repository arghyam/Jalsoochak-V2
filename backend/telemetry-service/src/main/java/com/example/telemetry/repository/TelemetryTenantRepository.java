package com.example.telemetry.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TelemetryTenantRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsSchemeById(String schemaName, Long schemeId) {
        validateSchemaName(schemaName);
        String sql = String.format("SELECT EXISTS (SELECT 1 FROM %s.scheme_master_table WHERE id = ?)", schemaName);
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, schemeId);
        return Boolean.TRUE.equals(exists);
    }

    public Optional<TelemetryOperator> findOperatorById(String schemaName, Long operatorId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT id, tenant_id, title, email, phone_number
                FROM %s.user_table
                WHERE id = ?
                LIMIT 1
                """, schemaName);
        List<TelemetryOperator> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TelemetryOperator(
                        toLong(rs.getObject("id")),
                        toInteger(rs.getObject("tenant_id")),
                        rs.getString("title"),
                        rs.getString("email"),
                        rs.getString("phone_number")
                ), operatorId);
        return rows.stream().findFirst();
    }

    public Optional<TelemetryOperatorWithSchema> findOperatorByPhoneAcrossTenants(String phoneNumber) {
        String schemaSql = """
                SELECT nspname
                FROM pg_namespace
                WHERE nspname LIKE 'tenant_%'
                ORDER BY nspname
                """;
        List<String> schemas = jdbcTemplate.query(schemaSql, (rs, n) -> rs.getString("nspname"));
        for (String schemaName : schemas) {
            Optional<TelemetryOperator> operator = findOperatorByPhone(schemaName, phoneNumber);
            if (operator.isPresent()) {
                return Optional.of(new TelemetryOperatorWithSchema(schemaName, operator.get()));
            }
        }
        return Optional.empty();
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
                                  String imageUrl) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                INSERT INTO %s.flow_reading_table
                    (scheme_id, reading_at, reading_date, extracted_reading, confirmed_reading,
                     correlation_id, quantity, channel, image_url, created_by, created_at, updated_by, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, NULL, ?, ?, NOW(), ?, NOW())
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
                imageUrl != null ? imageUrl : "",
                operatorId,
                operatorId
        );
        return id != null ? id.longValue() : null;
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

    private Optional<TelemetryOperator> findOperatorByPhone(String schemaName, String phoneNumber) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT id, tenant_id, title, email, phone_number
                FROM %s.user_table
                WHERE phone_number = ?
                LIMIT 1
                """, schemaName);
        List<TelemetryOperator> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TelemetryOperator(
                        toLong(rs.getObject("id")),
                        toInteger(rs.getObject("tenant_id")),
                        rs.getString("title"),
                        rs.getString("email"),
                        rs.getString("phone_number")
                ), phoneNumber);
        return rows.stream().findFirst();
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
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
