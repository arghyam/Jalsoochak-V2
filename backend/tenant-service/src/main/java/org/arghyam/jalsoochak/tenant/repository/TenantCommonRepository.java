package org.arghyam.jalsoochak.tenant.repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.arghyam.jalsoochak.tenant.dto.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.enums.TenantStatusEnum;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    /**
     * Row mapper for {@code common_schema.tenant_master_table}.
     */
    private static final RowMapper<TenantResponseDTO> TENANT_ROW_MAPPER = (rs, rowNum) -> TenantResponseDTO.builder()
            .id(rs.getInt("id"))
            .uuid(rs.getString("uuid"))
            .stateCode(rs.getString("state_code"))
            .lgdCode(rs.getInt("lgd_code"))
            .name(rs.getString("title"))
            .status(TenantStatusEnum.fromCode(rs.getInt("status")).name())
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime()
                    : null)
            .createdBy((Integer) rs.getObject("created_by"))
            .onboardedAt(rs.getTimestamp("onboarded_at") != null
                    ? rs.getTimestamp("onboarded_at").toLocalDateTime()
                    : null)
            .updatedAt(rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime()
                    : null)
            .updatedBy((Integer) rs.getObject("updated_by"))
            .build();

    /**
     * Inserts a new tenant into {@code common_schema.tenant_master_table}.
     */
    public Optional<TenantResponseDTO> createTenant(CreateTenantRequestDTO request, Integer currentUserId) {
        String sql = """
                INSERT INTO common_schema.tenant_master_table
                    (state_code, lgd_code, title, created_by, status, created_at)
                VALUES (?, ?, ?, ?, ?, NOW())
                RETURNING *
                """;

        List<TenantResponseDTO> results = jdbcTemplate.query(sql, TENANT_ROW_MAPPER,
                request.getStateCode(),
                request.getLgdCode(),
                request.getName(),
                currentUserId,
                TenantStatusEnum.ACTIVE.getCode());
        return results.stream().findFirst();
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

    /**
     * Finds a tenant by its state code.
     */
    public Optional<TenantResponseDTO> findByStateCode(String stateCode) {
        String sql = "SELECT * FROM common_schema.tenant_master_table WHERE state_code = ?";
        List<TenantResponseDTO> results = jdbcTemplate.query(sql, TENANT_ROW_MAPPER, stateCode);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Lists all tenants in the common_schema.tenant_master_table.
     */
    public List<TenantResponseDTO> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM common_schema.tenant_master_table ORDER BY id",
                TENANT_ROW_MAPPER);
    }

    /**
     * Finds a tenant by its ID.
     */
    public Optional<TenantResponseDTO> findById(Integer tenantId) {
        String sql = "SELECT * FROM common_schema.tenant_master_table WHERE id = ?";
        List<TenantResponseDTO> results = jdbcTemplate.query(sql, TENANT_ROW_MAPPER, tenantId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Updates tenant status. Only non-null fields are applied.
     */
    public Optional<TenantResponseDTO> updateTenant(Integer tenantId, UpdateTenantRequestDTO request,
            Integer currentUserId) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("UPDATE common_schema.tenant_master_table SET updated_at = NOW()");

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            TenantStatusEnum statusEnum = TenantStatusEnum.valueOf(request.getStatus().toUpperCase());
            sql.append(", status = ?");
            params.add(statusEnum.getCode());
        }

        sql.append(", updated_by = ?");
        params.add(currentUserId);

        sql.append(" WHERE id = ? RETURNING *");
        params.add(tenantId);

        List<TenantResponseDTO> results = jdbcTemplate.query(sql.toString(), TENANT_ROW_MAPPER, params.toArray());
        return results.stream().findFirst();
    }

    /**
     * Soft-deletes a tenant by setting status to INACTIVE and recording deleted_at.
     */
    public void deactivateTenant(Integer tenantId, Integer currentUserId) {
        String sql = """
                UPDATE common_schema.tenant_master_table
                SET status = ?, deleted_at = NOW(), updated_at = NOW(), deleted_by = ?, updated_by = ?
                WHERE id = ?
                """;
        int rows = jdbcTemplate.update(sql, TenantStatusEnum.INACTIVE.getCode(), currentUserId, currentUserId,
                tenantId);
        if (rows == 0) {
            throw new IllegalArgumentException("Tenant with tenantId " + tenantId + " does not exist");
        }
    }

    /**
     * Finds a tenant admin user by its UUID.
     */
    public Optional<Integer> findUserIdByUuid(String uuid) {
        if (uuid == null)
            return Optional.empty();
        String sql = "SELECT id FROM common_schema.tenant_admin_user_master_table WHERE uuid = ?";
        List<Integer> ids = jdbcTemplate.queryForList(sql, Integer.class, uuid);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    /**
     * Validates a schema name.
     */
    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }
}