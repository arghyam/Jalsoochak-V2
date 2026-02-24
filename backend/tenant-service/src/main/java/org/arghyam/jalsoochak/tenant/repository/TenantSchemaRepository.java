package org.arghyam.jalsoochak.tenant.repository;

import org.arghyam.jalsoochak.tenant.dto.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.DepartmentResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for tenant-schema-scoped queries.
 * <p>
 * Every method accepts the target schema name explicitly so that the
 * caller (service layer) controls which tenant's data is accessed.
 * Schema names are validated before being interpolated into SQL to
 * prevent injection.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<DepartmentResponseDTO> DEPARTMENT_ROW_MAPPER = (rs, rowNum) ->
            DepartmentResponseDTO.builder()
                    .id(rs.getInt("id"))
                    .uuid(rs.getString("uuid"))
                    .title(rs.getString("title"))
                    .departmentLocationConfigId((Integer) rs.getObject("department_location_config_id"))
                    .parentId((Integer) rs.getObject("parent_id"))
                    .status(rs.getInt("status"))
                    .createdAt(rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                    .createdBy((Integer) rs.getObject("created_by"))
                    .updatedAt(rs.getTimestamp("updated_at") != null
                            ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                    .updatedBy((Integer) rs.getObject("updated_by"))
                    .build();

    public List<DepartmentResponseDTO> getDepartments(String schemaName) {
        validateSchemaName(schemaName);
        log.debug("Fetching departments from schema: {}", schemaName);

        String sql = String.format(
                "SELECT * FROM %s.department_location_master_table ORDER BY id", schemaName);
        return jdbcTemplate.query(sql, DEPARTMENT_ROW_MAPPER);
    }

    public DepartmentResponseDTO createDepartment(String schemaName, CreateDepartmentRequestDTO request) {
        validateSchemaName(schemaName);
        log.debug("Creating department in schema: {}", schemaName);

        String sql = String.format("""
                INSERT INTO %s.department_location_master_table
                    (title, department_location_config_id, parent_id, status, created_by)
                VALUES (?, ?, ?, ?, ?)
                RETURNING *
                """, schemaName);

        return jdbcTemplate.queryForObject(sql, DEPARTMENT_ROW_MAPPER,
                request.getTitle(),
                request.getDepartmentLocationConfigId(),
                request.getParentId(),
                request.getStatus(),
                request.getCreatedBy());
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }
}
