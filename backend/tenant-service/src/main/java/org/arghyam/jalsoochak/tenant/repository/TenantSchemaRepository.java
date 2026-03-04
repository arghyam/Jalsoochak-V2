package org.arghyam.jalsoochak.tenant.repository;

import java.util.List;
import java.util.Optional;

import org.arghyam.jalsoochak.tenant.dto.request.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.LocationResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LanguageConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelNameDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationConfigDTO;
import org.arghyam.jalsoochak.tenant.enums.RegionTypeEnum;
import org.arghyam.jalsoochak.tenant.exception.InvalidConfigValueException;

import java.sql.Types;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
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
         * Row mapper for {@code tenant_schema.lgd_location_master_table}.
         */
        private static final RowMapper<LocationResponseDTO> LGD_LOCATION_ROW_MAPPER = (rs,
                        rowNum) -> LocationResponseDTO.builder()
                                        .id(rs.getInt("id"))
                                        .uuid(rs.getString("uuid"))
                                        .title(rs.getString("title"))
                                        .lgdCode(rs.getString("lgd_code"))
                                        .parentId((Integer) rs.getObject("parent_id"))
                                        .status(rs.getInt("status"))
                                        .build();

        /**
         * Row mapper for {@code tenant_schema.department_location_master_table} as LocationResponseDTO.
         */
        private static final RowMapper<LocationResponseDTO> DEPT_LOCATION_ROW_MAPPER = (rs,
                        rowNum) -> LocationResponseDTO.builder()
                                        .id(rs.getInt("id"))
                                        .uuid(rs.getString("uuid"))
                                        .title(rs.getString("title"))
                                        .parentId((Integer) rs.getObject("parent_id"))
                                        .status(rs.getInt("status"))
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
                if (languages == null) {
                        throw new InvalidConfigValueException("Languages cannot be null");
                }
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
        public LocationConfigDTO getLocationHierarchy(String schemaName, RegionTypeEnum regionType) {
                validateSchemaName(schemaName);
                String sql = String.format(
                                "SELECT level, level_name FROM %s.location_config_master_table WHERE region_type = ? ORDER BY level",
                                schemaName);
                List<LocationLevelConfigDTO> levels = jdbcTemplate.query(sql, (rs, rowNum) -> {
                        try {
                                List<LocationLevelNameDTO> names = objectMapper.readValue(
                                        rs.getString("level_name"),
                                        objectMapper.getTypeFactory().constructCollectionType(List.class, LocationLevelNameDTO.class));
                                return LocationLevelConfigDTO.builder()
                                        .level(rs.getInt("level"))
                                        .levelName(names)
                                        .build();
                        } catch (JsonProcessingException e) {
                                throw new InvalidConfigValueException("Malformed level_name JSON in location_config_master_table", e);
                        }
                }, regionType.getCode());
                return LocationConfigDTO.builder().locationHierarchy(levels).build();
        }

        /**
         * Replaces location hierarchy configuration for a given region type.
         * Deletes all existing entries for the region type, then inserts the new ones.
         */
        public void setLocationHierarchy(String schemaName, RegionTypeEnum regionType,
                        List<LocationLevelConfigDTO> hierarchy,
                        Integer currentUserId) {
                validateSchemaName(schemaName);
                if (hierarchy == null) {
                        throw new InvalidConfigValueException("Hierarchy cannot be null");
                }

                String deleteSql = String.format(
                                "DELETE FROM %s.location_config_master_table WHERE region_type = ?",
                                schemaName);
                jdbcTemplate.update(deleteSql, regionType.getCode());

                String insertSql = String.format(
                                """
                                                INSERT INTO %s.location_config_master_table (region_type, level, level_name, created_by, updated_by)
                                                VALUES (?, ?, ?, ?, ?)
                                                """,
                                schemaName);

                for (LocationLevelConfigDTO config : hierarchy) {
                        try {
                                String levelNameJson = objectMapper.writeValueAsString(config.getLevelName());
                                jdbcTemplate.update(insertSql, ps -> {
                                        ps.setObject(1, regionType.getCode());
                                        ps.setInt(2, config.getLevel());
                                        ps.setObject(3, levelNameJson, Types.OTHER);
                                        ps.setObject(4, currentUserId);
                                        ps.setObject(5, currentUserId);
                                });
                        } catch (JsonProcessingException e) {
                                throw new InvalidConfigValueException("Failed to serialize levelName for level " + config.getLevel(), e);
                        }
                }
        }

        /**
         * Fetches child locations by parent ID from lgd_location_master_table.
         * 
         * @param schemaName Schema name (e.g., "tenant_mp")
         * @param parentId   Parent location ID (or null for root-level locations)
         * @return List of child locations ordered by title
         */
        public List<LocationResponseDTO> findLgdLocationsByParentId(String schemaName, Integer parentId) {
                validateSchemaName(schemaName);
                
                String sql;
                Object[] params;
                
                if (parentId == null) {
                        sql = String.format(
                                "SELECT * FROM %s.lgd_location_master_table WHERE parent_id IS NULL AND status = 1 ORDER BY title",
                                schemaName);
                        params = new Object[]{};
                } else {
                        sql = String.format(
                                "SELECT * FROM %s.lgd_location_master_table WHERE parent_id = ? AND status = 1 ORDER BY title",
                                schemaName);
                        params = new Object[]{parentId};
                }
                
                log.debug("Fetching LGD locations from schema: {} with parentId: {}", schemaName, parentId);
                return jdbcTemplate.query(sql, LGD_LOCATION_ROW_MAPPER, params);
        }

        /**
         * Fetches child locations by parent ID from department_location_master_table.
         * 
         * @param schemaName Schema name (e.g., "tenant_mp")
         * @param parentId   Parent location ID (or null for root-level locations)
         * @return List of child locations ordered by title
         */
        public List<LocationResponseDTO> findDepartmentLocationsByParentId(String schemaName, Integer parentId) {
                validateSchemaName(schemaName);
                
                String sql;
                Object[] params;
                
                if (parentId == null) {
                        sql = String.format(
                                "SELECT * FROM %s.department_location_master_table WHERE parent_id IS NULL AND status = 1 ORDER BY title",
                                schemaName);
                        params = new Object[]{};
                } else {
                        sql = String.format(
                                "SELECT * FROM %s.department_location_master_table WHERE parent_id = ? AND status = 1 ORDER BY title",
                                schemaName);
                        params = new Object[]{parentId};
                }
                
                log.debug("Fetching department locations from schema: {} with parentId: {}", schemaName, parentId);
                return jdbcTemplate.query(sql, DEPT_LOCATION_ROW_MAPPER, params);
        }
}

