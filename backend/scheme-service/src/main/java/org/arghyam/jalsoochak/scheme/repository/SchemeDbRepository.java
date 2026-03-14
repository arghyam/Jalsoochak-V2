package org.arghyam.jalsoochak.scheme.repository;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.scheme.dto.SchemeDTO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
public class SchemeDbRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final Pattern SAFE_SCHEMA = Pattern.compile("^[a-z_][a-z0-9_]*$");

    public List<SchemeDTO> findAllSchemes(String schemaName) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT id, uuid, state_scheme_id, centre_scheme_id, scheme_name,
                       fhtc_count, planned_fhtc, house_hold_count,
                       latitude, longitude, channel, work_status, operating_status
                FROM %s.scheme_master_table
                WHERE deleted_at IS NULL
                ORDER BY id DESC
                """, schemaName);

        return jdbcTemplate.query(sql, (rs, rowNum) -> SchemeDTO.builder()
                .id(rs.getInt("id"))
                .uuid(rs.getString("uuid"))
                .stateSchemeId(rs.getString("state_scheme_id"))
                .centreSchemeId(rs.getString("centre_scheme_id"))
                .schemeName(rs.getString("scheme_name"))
                .fhtcCount(rs.getInt("fhtc_count"))
                .plannedFhtc(rs.getInt("planned_fhtc"))
                .houseHoldCount(rs.getInt("house_hold_count"))
                .latitude((Double) rs.getObject("latitude"))
                .longitude((Double) rs.getObject("longitude"))
                .channel(rs.getInt("channel"))
                .workStatus(rs.getInt("work_status"))
                .operatingStatus(rs.getInt("operating_status"))
                .build());
    }

    public boolean existsSchemeById(String schemaName, Integer schemeId) {
        validateSchemaName(schemaName);
        String sql = String.format(
                "SELECT EXISTS (SELECT 1 FROM %s.scheme_master_table WHERE id = ? AND deleted_at IS NULL)",
                schemaName
        );
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, schemeId);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Batch existence check for large uploads (avoids N queries).
     */
    public Set<Integer> findExistingSchemeIds(String schemaName, List<Integer> schemeIds) {
        return findExistingIds(schemaName, "scheme_master_table", schemeIds);
    }

    public boolean existsLgdLocationById(String schemaName, Integer lgdId) {
        validateSchemaName(schemaName);
        String sql = String.format(
                "SELECT EXISTS (SELECT 1 FROM %s.lgd_location_master_table WHERE id = ? AND deleted_at IS NULL)",
                schemaName
        );
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, lgdId);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Batch existence check for large uploads (avoids N queries).
     */
    public Set<Integer> findExistingLgdLocationIds(String schemaName, List<Integer> lgdIds) {
        return findExistingIds(schemaName, "lgd_location_master_table", lgdIds);
    }

    public boolean existsDepartmentLocationById(String schemaName, Integer departmentId) {
        validateSchemaName(schemaName);
        String sql = String.format(
                "SELECT EXISTS (SELECT 1 FROM %s.department_location_master_table WHERE id = ? AND deleted_at IS NULL)",
                schemaName
        );
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, departmentId);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Batch existence check for large uploads (avoids N queries).
     */
    public Set<Integer> findExistingDepartmentLocationIds(String schemaName, List<Integer> departmentIds) {
        return findExistingIds(schemaName, "department_location_master_table", departmentIds);
    }

    /**
     * Batch lookup of internal scheme IDs by state_scheme_id (case-insensitive).
     * Returns a map keyed by lower(state_scheme_id).
     */
    public Map<String, Integer> findSchemeIdsByStateSchemeIds(String schemaName, List<String> stateSchemeIds) {
        return findIdsByLowerTextKey(schemaName, "scheme_master_table", "state_scheme_id", stateSchemeIds);
    }

    /**
     * Batch lookup of LGD location IDs by lgd_code (case-insensitive).
     * Returns a map keyed by lower(lgd_code).
     */
    public Map<String, Integer> findLgdIdsByCodes(String schemaName, List<String> lgdCodes) {
        return findIdsByLowerTextKey(schemaName, "lgd_location_master_table", "lgd_code", lgdCodes);
    }

    /**
     * Batch lookup of department location IDs by title (case-insensitive).
     * Returns a map keyed by lower(title).
     */
    public Map<String, Integer> findDepartmentIdsByTitles(String schemaName, List<String> titles) {
        return findIdsByLowerTextKey(schemaName, "department_location_master_table", "title", titles);
    }

    public Integer findUserIdByEmail(String schemaName, String email) {
        validateSchemaName(schemaName);
        if (email == null || email.isBlank()) {
            return null;
        }

        String sql = String.format("""
                SELECT id
                FROM %s.user_table
                WHERE lower(email) = lower(?)
                  AND deleted_at IS NULL
                """, schemaName);

        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, email);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public boolean isUserStateAdmin(String schemaName, Integer userId) {
        validateSchemaName(schemaName);
        if (userId == null) {
            return false;
        }

        String sql = String.format("""
                SELECT EXISTS (
                    SELECT 1
                    FROM %s.user_table u
                    WHERE u.id = ?
                      AND u.user_type = (
                          SELECT ut.id
                          FROM common_schema.user_type_master_table ut
                          WHERE lower(ut.c_name) = lower('STATE_ADMIN')
                          LIMIT 1
                      )
                )
                """, schemaName);

        Boolean ok = jdbcTemplate.queryForObject(sql, Boolean.class, userId);
        return Boolean.TRUE.equals(ok);
    }

    public void insertSchemes(String schemaName, List<SchemeCreateRecord> rows) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                INSERT INTO %s.scheme_master_table
                    (uuid, state_scheme_id, centre_scheme_id, scheme_name,
                     fhtc_count, planned_fhtc, house_hold_count,
                     latitude, longitude, channel, work_status, operating_status,
                     created_at, created_by, updated_at, updated_by, deleted_at, deleted_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, NOW(), ?, NULL, NULL)
                """, schemaName);

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SchemeCreateRecord row = rows.get(i);
                ps.setString(1, row.uuid());
                ps.setString(2, row.stateSchemeId());
                ps.setString(3, row.centreSchemeId());
                ps.setString(4, row.schemeName());
                ps.setInt(5, row.fhtcCount());
                ps.setInt(6, row.plannedFhtc());
                ps.setInt(7, row.houseHoldCount());
                ps.setObject(8, row.latitude());
                ps.setObject(9, row.longitude());
                ps.setObject(10, row.channel(), Types.INTEGER);
                ps.setInt(11, row.workStatus());
                ps.setInt(12, row.operatingStatus());
                ps.setInt(13, row.createdBy());
                ps.setInt(14, row.updatedBy());
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    public void insertLgdMappings(String schemaName, List<SchemeLgdMappingCreateRecord> rows) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                INSERT INTO %s.scheme_lgd_mapping_table
                    (scheme_id, parent_lgd_id, parent_lgd_level, created_by, created_at, updated_by, updated_at, deleted_at, deleted_by)
                VALUES (?, ?, ?, ?, NOW(), ?, NOW(), NULL, NULL)
                """, schemaName);

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SchemeLgdMappingCreateRecord row = rows.get(i);
                ps.setInt(1, row.schemeId());
                ps.setInt(2, row.parentLgdId());
                ps.setString(3, String.valueOf(row.parentLgdLevel()));
                ps.setInt(4, row.createdBy());
                ps.setInt(5, row.updatedBy());
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    public void insertSubdivisionMappings(String schemaName, List<SchemeSubdivisionMappingCreateRecord> rows) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                INSERT INTO %s.scheme_department_mapping_table
                    (scheme_id, parent_department_id, parent_department_level, created_by, created_at, updated_by, updated_at, deleted_at, deleted_by)
                VALUES (?, ?, ?, ?, NOW(), ?, NOW(), NULL, NULL)
                """, schemaName);

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SchemeSubdivisionMappingCreateRecord row = rows.get(i);
                ps.setInt(1, row.schemeId());
                ps.setInt(2, row.parentDepartmentId());
                ps.setString(3, row.parentDepartmentLevel());
                ps.setInt(4, row.createdBy());
                ps.setInt(5, row.updatedBy());
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank() || !SAFE_SCHEMA.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    private Set<Integer> findExistingIds(String schemaName, String table, List<Integer> ids) {
        validateSchemaName(schemaName);
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }

        // Deduplicate to keep the IN clause small and stable.
        Set<Integer> uniq = new HashSet<>(Math.max(16, ids.size()));
        for (Integer id : ids) {
            if (id != null) {
                uniq.add(id);
            }
        }
        if (uniq.isEmpty()) {
            return Set.of();
        }

        StringBuilder placeholders = new StringBuilder();
        int n = uniq.size();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                placeholders.append(',');
            }
            placeholders.append('?');
        }

        String sql = String.format(
                "SELECT id FROM %s.%s WHERE deleted_at IS NULL AND id IN (%s)",
                schemaName,
                table,
                placeholders
        );
        Object[] args = uniq.toArray();
        List<Integer> existing = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("id"), args);
        return new HashSet<>(existing);
    }

    private Map<String, Integer> findIdsByLowerTextKey(String schemaName, String table, String keyColumn, List<String> values) {
        validateSchemaName(schemaName);
        if (values == null || values.isEmpty()) {
            return Map.of();
        }

        // Deduplicate normalized values to keep IN clause small and stable.
        Set<String> uniq = new HashSet<>(Math.max(16, values.size()));
        for (String v : values) {
            if (v == null) {
                continue;
            }
            String t = v.trim();
            if (!t.isBlank()) {
                uniq.add(t.toLowerCase(Locale.ROOT));
            }
        }
        if (uniq.isEmpty()) {
            return Map.of();
        }

        StringBuilder placeholders = new StringBuilder();
        int n = uniq.size();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                placeholders.append(',');
            }
            placeholders.append('?');
        }

        String sql = String.format(
                "SELECT lower(%s) AS k, id FROM %s.%s WHERE deleted_at IS NULL AND lower(%s) IN (%s)",
                keyColumn,
                schemaName,
                table,
                keyColumn,
                placeholders
        );
        Object[] args = uniq.toArray();

        Map<String, Integer> out = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            String k = rs.getString("k");
            int id = rs.getInt("id");
            // If there are duplicates in DB (case-insensitive), keep the first deterministically.
            out.putIfAbsent(k, id);
        }, args);
        return out;
    }
}
