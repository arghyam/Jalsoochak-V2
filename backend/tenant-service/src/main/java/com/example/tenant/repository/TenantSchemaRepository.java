package com.example.tenant.repository;

import com.example.tenant.dto.DepartmentResponse;
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

    private static final RowMapper<DepartmentResponse> DEPARTMENT_ROW_MAPPER = (rs, rowNum) ->
            DepartmentResponse.builder()
                    .id(rs.getInt("id"))
                    .uuid(rs.getString("uuid"))
                    .title(rs.getString("title"))
                    .departmentLocationTypeLevel(rs.getInt("department_location_type_level"))
                    .departmentLocationTypeName(rs.getString("department_location_type_name"))
                    .parentId((Integer) rs.getObject("parent_id"))
                    .status(rs.getString("status"))
                    .createdAt(rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                    .createdBy((Integer) rs.getObject("created_by"))
                    .updatedAt(rs.getTimestamp("updated_at") != null
                            ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                    .updatedBy((Integer) rs.getObject("updated_by"))
                    .build();

    /**
     * Fetches all departments from the specified tenant schema's
     * {@code department_master_table}.
     *
     * @param schemaName validated tenant schema name (e.g. {@code tenant_mp})
     */
    public List<DepartmentResponse> getDepartments(String schemaName) {
        validateSchemaName(schemaName);
        log.debug("Fetching departments from schema: {}", schemaName);

        String sql = String.format(
                "SELECT * FROM %s.department_master_table ORDER BY id", schemaName);
        return jdbcTemplate.query(sql, DEPARTMENT_ROW_MAPPER);
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }
}
