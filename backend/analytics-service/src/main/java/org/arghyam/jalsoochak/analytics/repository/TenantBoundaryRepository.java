package org.arghyam.jalsoochak.analytics.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class TenantBoundaryRepository {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getMergedBoundaryForTenant(String schemaName) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT
                    COUNT(*)::int AS boundary_count,
                    ST_AsGeoJSON(
                        ST_UnaryUnion(
                            ST_Collect(l.geom)
                        )
                    ) AS boundary_geojson
                FROM %1$s.lgd_location_master_table l
                JOIN %1$s.location_config_master_table c
                    ON c.id = l.lgd_location_config_id
                WHERE c.level = 2
                  AND l.geom IS NOT NULL
                  AND l.deleted_at IS NULL
                  AND c.deleted_at IS NULL
                  AND COALESCE(l.status, 1) = 1
                """, schemaName);
        return jdbcTemplate.queryForMap(sql);
    }

    public Integer getLocationLevel(String schemaName, Integer lgdId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT c.level
                FROM %1$s.lgd_location_master_table l
                JOIN %1$s.location_config_master_table c
                    ON c.id = l.lgd_location_config_id
                WHERE l.id = ?
                  AND l.deleted_at IS NULL
                  AND c.deleted_at IS NULL
                LIMIT 1
                """, schemaName);
        List<Integer> rows = jdbcTemplate.query(sql, (rs, n) -> (Integer) rs.getObject("level"), lgdId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> getChildLevelByParent(String schemaName, Integer parentLgdId, Integer tenantId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT
                    l.id AS lgd_id,
                    l.parent_id AS parent_lgd_id,
                    c.level AS child_level,
                    (
                        SELECT COUNT(*)
                        FROM analytics_schema.dim_scheme_table s
                        WHERE s.tenant_id = ?
                          AND (
                              (c.level = 1 AND s.level_1_lgd_id = l.id) OR
                              (c.level = 2 AND s.level_2_lgd_id = l.id) OR
                              (c.level = 3 AND s.level_3_lgd_id = l.id) OR
                              (c.level = 4 AND s.level_4_lgd_id = l.id) OR
                              (c.level = 5 AND s.level_5_lgd_id = l.id) OR
                              (c.level = 6 AND s.level_6_lgd_id = l.id)
                          )
                    )::int AS scheme_count,
                    l.title,
                    l.lgd_code,
                    ST_AsGeoJSON(l.geom) AS boundary_geojson
                FROM %1$s.lgd_location_master_table l
                JOIN %1$s.location_config_master_table c
                    ON c.id = l.lgd_location_config_id
                WHERE l.parent_id = ?
                  AND l.deleted_at IS NULL
                  AND c.deleted_at IS NULL
                  AND COALESCE(l.status, 1) = 1
                ORDER BY l.id
                """, schemaName);
        return jdbcTemplate.queryForList(sql, tenantId, parentLgdId);
    }

    public Map<String, Object> getMergedBoundaryByParent(String schemaName, Integer parentLgdId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT
                    COUNT(*)::int AS child_count,
                    ST_AsGeoJSON(
                        ST_UnaryUnion(
                            ST_Collect(l.geom) FILTER (WHERE l.geom IS NOT NULL)
                        )
                    ) AS boundary_geojson
                FROM %1$s.lgd_location_master_table l
                JOIN %1$s.location_config_master_table c
                    ON c.id = l.lgd_location_config_id
                WHERE l.parent_id = ?
                  AND l.deleted_at IS NULL
                  AND c.deleted_at IS NULL
                  AND COALESCE(l.status, 1) = 1
                """, schemaName);
        return jdbcTemplate.queryForMap(sql, parentLgdId);
    }

    public boolean tableExists(String schemaName, String tableName) {
        validateSchemaName(schemaName);
        validateTableName(tableName);
        String sql = "SELECT to_regclass(?) IS NOT NULL";
        String qualifiedTableName = schemaName + "." + tableName;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, qualifiedTableName);
        return Boolean.TRUE.equals(exists);
    }

    public boolean columnExists(String schemaName, String tableName, String columnName) {
        validateSchemaName(schemaName);
        validateTableName(tableName);
        validateTableName(columnName);
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

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    private void validateTableName(String tableName) {
        if (tableName == null || !tableName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
    }
}
