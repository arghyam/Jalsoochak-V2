package org.arghyam.jalsoochak.scheme.repository;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.scheme.dto.SchemeDTO;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SchemeDbRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<SchemeDTO> findAllSchemes() {
        String sql = """
                SELECT id, uuid, state_scheme_id, centre_scheme_id, scheme_name,
                       fhtc_count, planned_fhtc, house_hold_count,
                       latitude, longitude, channel, work_status, operating_status
                FROM scheme_master_table
                WHERE deleted_at IS NULL
                ORDER BY id DESC
                """;

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

    public boolean existsSchemeById(Integer schemeId) {
        String sql = "SELECT EXISTS (SELECT 1 FROM scheme_master_table WHERE id = ? AND deleted_at IS NULL)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, schemeId);
        return Boolean.TRUE.equals(exists);
    }

    public void insertSchemes(List<SchemeCreateRecord> rows) {
        String sql = """
                INSERT INTO scheme_master_table
                    (uuid, state_scheme_id, centre_scheme_id, scheme_name,
                     fhtc_count, planned_fhtc, house_hold_count,
                     latitude, longitude, channel, work_status, operating_status,
                     created_at, created_by, updated_at, updated_by, deleted_at, deleted_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, NOW(), ?, NULL, NULL)
                """;

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
                ps.setInt(10, row.channel());
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

    public void insertVillageMappings(List<SchemeVillageMappingCreateRecord> rows) {
        String sql = """
                INSERT INTO scheme_lgd_mapping_table
                    (scheme_id, parent_lgd_id, parent_lgd_level, created_by, created_at, updated_by, updated_at, deleted_at, deleted_by)
                VALUES (?, ?, ?, ?, NOW(), ?, NOW(), NULL, NULL)
                """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SchemeVillageMappingCreateRecord row = rows.get(i);
                ps.setInt(1, row.schemeId());
                ps.setInt(2, row.parentLgdId());
                ps.setString(3, row.parentLgdLevel());
                ps.setInt(4, row.createdBy());
                ps.setInt(5, row.updatedBy());
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    public void insertSubdivisionMappings(List<SchemeSubdivisionMappingCreateRecord> rows) {
        String sql = """
                INSERT INTO scheme_department_mapping_table
                    (scheme_id, parent_department_id, parent_department_level, created_by, created_at, updated_by, updated_at, deleted_at, deleted_by)
                VALUES (?, ?, ?, ?, NOW(), ?, NOW(), NULL, NULL)
                """;

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
}
