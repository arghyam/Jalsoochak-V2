package org.arghyam.jalsoochak.tenant.repository;

import java.util.List;
import java.util.Optional;

import org.arghyam.jalsoochak.tenant.dto.request.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LanguageConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelNameDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.TenantLocationHierarchyConfigDTO;
import org.arghyam.jalsoochak.tenant.enums.RegionTypeEnum;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        private final ObjectMapper objectMapper;

        /**
         * Row mapper for {@code tenant_schema.department_location_master_table}.
         */
        private static final RowMapper<DepartmentResponseDTO> DEPARTMENT_ROW_MAPPER = (rs,
                        rowNum) -> DepartmentResponseDTO.builder()
                                        .id(rs.getInt("id"))
                                        .uuid(rs.getString("uuid"))
                                        .title(rs.getString("title"))
                                        .departmentLocationConfigId(
                                                        (Integer) rs.getObject("department_location_config_id"))
                                        .parentId((Integer) rs.getObject("parent_id"))
                                        .status(rs.getInt("status"))
                                        .createdAt(rs.getTimestamp("created_at") != null
                                                        ? rs.getTimestamp("created_at").toLocalDateTime()
                                                        : null)
                                        .createdBy((Integer) rs.getObject("created_by"))
                                        .updatedAt(rs.getTimestamp("updated_at") != null
                                                        ? rs.getTimestamp("updated_at").toLocalDateTime()
                                                        : null)
                                        .updatedBy((Integer) rs.getObject("updated_by"))
                                        .build();

        /**
         * Fetches all departments from the specified schema.
         */
        public List<DepartmentResponseDTO> getDepartments(String schemaName) {
                validateSchemaName(schemaName);
                log.debug("Fetching departments from schema: {}", schemaName);

                String sql = String.format(
                                "SELECT * FROM %s.department_location_master_table ORDER BY id", schemaName);
                return jdbcTemplate.query(sql, DEPARTMENT_ROW_MAPPER);
        }

        /**
         * Creates a new department in the specified schema.
         */
        public Optional<DepartmentResponseDTO> createDepartment(String schemaName, CreateDepartmentRequestDTO request,
                        Integer currentUserId) {
                validateSchemaName(schemaName);
                log.debug("Creating department in schema: {}", schemaName);

                String sql = String.format("""
                                INSERT INTO %s.department_location_master_table
                                    (title, department_location_config_id, parent_id, status, created_by, updated_by)
                                VALUES (?, ?, ?, ?, ?, ?)
                                RETURNING *
                                """, schemaName);

                List<DepartmentResponseDTO> results = jdbcTemplate.query(sql, DEPARTMENT_ROW_MAPPER,
                                request.getTitle(),
                                request.getDepartmentLocationConfigId(),
                                request.getParentId(),
                                request.getStatus(),
                                currentUserId,
                                currentUserId);
                return results.stream().findFirst();
        }

        /**
         * Validates the schema name.
         */
        private void validateSchemaName(String schemaName) {
                if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
                        throw new IllegalArgumentException("Invalid schema name: " + schemaName);
                }
        }

        /**
         * Fetches all supported languages from the specified schema.
         */
        public List<LanguageConfigDTO> getSupportedLanguages(String schemaName) {
                validateSchemaName(schemaName);
                String sql = String.format(
                                "SELECT language_name, preference FROM %s.language_master_table WHERE status = 1 ORDER BY preference",
                                schemaName);
                return jdbcTemplate.query(sql, (rs, rowNum) -> LanguageConfigDTO.builder()
                                .language(rs.getString("language_name"))
                                .preference((Integer) rs.getObject("preference"))
                                .build());
        }

        /**
         * Updates supported languages for a tenant by synchronizing the
         * language_master_table.
         * mark all as inactive (status=0) and then upsert new ones.
         */
        public void setSupportedLanguages(String schemaName, List<LanguageConfigDTO> languages, Integer currentUserId) {
                validateSchemaName(schemaName);
                // Mark all as inactive
                String deactivateSql = String.format(
                                "UPDATE %s.language_master_table SET status = 0, updated_by = ?, updated_at = NOW()",
                                schemaName);
                jdbcTemplate.update(deactivateSql, currentUserId);

                // Upsert new languages
                String upsertSql = String.format(
                                """
                                                INSERT INTO %s.language_master_table (language_name, preference, status, created_by, updated_by)
                                                VALUES (?, ?, 1, ?, ?)
                                                ON CONFLICT (language_name) DO UPDATE SET status = 1, preference = EXCLUDED.preference, updated_by = EXCLUDED.updated_by, updated_at = NOW()
                                                """,
                                schemaName);

                for (LanguageConfigDTO lang : languages) {
                        jdbcTemplate.update(upsertSql, lang.getLanguage(), lang.getPreference(), currentUserId,
                                        currentUserId);
                }
        }

        /**
         * Fetches location hierarchy configuration from the specified schema for a
         * given region type.
         */
        public TenantLocationHierarchyConfigDTO getLocationHierarchy(String schemaName, RegionTypeEnum regionType) {
                validateSchemaName(schemaName);
                String sql = String.format(
                                "SELECT level, level_name FROM %s.location_config_master_table WHERE region_type = ? ORDER BY level",
                                schemaName);
                List<LocationLevelConfigDTO> levels = jdbcTemplate.query(sql, (rs, rowNum) -> {
                        try {
                                List<LocationLevelNameDTO> names = objectMapper.readValue(rs.getString("level_name"),
                                                new com.fasterxml.jackson.core.type.TypeReference<List<LocationLevelNameDTO>>() {
                                                });
                                return LocationLevelConfigDTO.builder()
                                                .level(rs.getInt("level"))
                                                .levelName(names)
                                                .build();
                        } catch (Exception e) {
                                throw new RuntimeException("Failed to parse level_name JSON", e);
                        }
                }, regionType.getCode());
                return TenantLocationHierarchyConfigDTO.builder().locationHierarchy(levels).build();
        }

        /**
         * Updates location hierarchy configuration for a given region type.
         * Perpetuates the hierarchy by upserting levels.
         */
        public void setLocationHierarchy(String schemaName, RegionTypeEnum regionType,
                        List<LocationLevelConfigDTO> hierarchy,
                        Integer currentUserId) {
                validateSchemaName(schemaName);

                String upsertSql = String.format(
                                """
                                                INSERT INTO %s.location_config_master_table (region_type, level, level_name, created_by, updated_by)
                                                VALUES (?, ?, ?::jsonb, ?, ?)
                                                ON CONFLICT (region_type, level) DO UPDATE SET
                                                    level_name = EXCLUDED.level_name,
                                                    updated_by = EXCLUDED.updated_by,
                                                    updated_at = NOW()
                                                """,
                                schemaName);

                for (LocationLevelConfigDTO config : hierarchy) {
                        try {
                                String levelNameJson = objectMapper.writeValueAsString(config.getLevelName());
                                jdbcTemplate.update(upsertSql, regionType.getCode(), config.getLevel(), levelNameJson,
                                                currentUserId,
                                                currentUserId);
                        } catch (Exception e) {
                                throw new RuntimeException("Failed to serialize level_name JSON", e);
                        }
                }
        }
}