package com.example.tenant.repository;

import com.example.tenant.dto.CreateTenantRequestDTO;
import com.example.tenant.dto.TenantResponseDTO;
import com.example.tenant.enums.TenantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

/**
 * Repository for operations on {@code common_schema} tables.
 * Uses {@link JdbcTemplate} with explicit schema-qualified SQL
 * to avoid dependence on the connection's current {@code search_path}.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class TenantCommonRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<TenantResponseDTO> TENANT_ROW_MAPPER = (rs, rowNum) ->
            TenantResponseDTO.builder()
                    .id(rs.getInt("id"))
                    .uuid(rs.getString("uuid"))
                    .stateCode(rs.getString("state_code"))
                    .lgdCode(rs.getInt("lgd_code"))
                    .name(rs.getString("title"))
                    .status(TenantStatus.fromCode(rs.getInt("status")).name())
                    .createdAt(rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                    .createdBy((Integer) rs.getObject("created_by"))
                    .onboardedAt(rs.getTimestamp("onboarded_at") != null
                            ? rs.getTimestamp("onboarded_at").toLocalDateTime() : null)
                    .updatedAt(rs.getTimestamp("updated_at") != null
                            ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                    .updatedBy((Integer) rs.getObject("updated_by"))
                    .build();

    /**
     * Inserts a new tenant into {@code common_schema.tenant_master_table}.
     */
    public TenantResponseDTO createTenant(CreateTenantRequestDTO request) {
        String sql = """
                INSERT INTO common_schema.tenant_master_table
                    (state_code, lgd_code, title, created_by, status)
                VALUES (?, ?, ?, ?, ?)
                RETURNING *
                """;

        return jdbcTemplate.queryForObject(sql, TENANT_ROW_MAPPER,
                request.getStateCode(),
                request.getLgdCode(),
                request.getName(),
                request.getCreatedBy(),
                TenantStatus.ACTIVE.getCode());
    }

    /**
     * Calls the {@code create_tenant_schema()} PL/pgSQL function to provision
     * all tenant-specific tables and indexes in a new schema.
     */
    public void provisionTenantSchema(String schemaName) {
        validateSchemaName(schemaName);
        log.info("Provisioning tenant schema: {}", schemaName);

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT create_tenant_schema(?)")) {
                ps.setString(1, schemaName);
                ps.execute();
            }
            return null;
        });
    }

    public Optional<TenantResponseDTO> findByStateCode(String stateCode) {
        String sql = "SELECT * FROM common_schema.tenant_master_table WHERE state_code = ?";
        List<TenantResponseDTO> results = jdbcTemplate.query(sql, TENANT_ROW_MAPPER, stateCode);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<TenantResponseDTO> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM common_schema.tenant_master_table ORDER BY id",
                TENANT_ROW_MAPPER);
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }
}
