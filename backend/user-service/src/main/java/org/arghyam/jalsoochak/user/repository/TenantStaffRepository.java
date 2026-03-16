package org.arghyam.jalsoochak.user.repository;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.dto.response.RoleCountDTO;
import org.arghyam.jalsoochak.user.dto.response.TenantStaffResponseDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TenantStaffRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TenantStaffResponseDTO> staffRowMapper = (rs, rowNum) -> TenantStaffResponseDTO.builder()
            .id(rs.getLong("id"))
            .uuid(rs.getString("uuid"))
            .title(rs.getString("title"))
            .email(rs.getString("email"))
            .phoneNumber(rs.getString("phone_number"))
            .status((Integer) rs.getObject("status"))
            .role(rs.getString("role"))
            .build();

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    public List<TenantStaffResponseDTO> listStaff(
            String schemaName,
            String role,
            Integer status,
            String name,
            String sortBy,
            String sortDir,
            int offset,
            int limit
    ) {
        validateSchemaName(schemaName);

        SqlAndArgs where = buildWhere(role, status, name);
        String orderBy = orderBy(sortBy, sortDir);

        String sql = String.format("""
                SELECT u.id,
                       u.uuid,
                       u.title,
                       u.email,
                       u.phone_number,
                       u.status,
                       ut.c_name AS role
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                WHERE u.deleted_at IS NULL
                  %s
                %s
                LIMIT ? OFFSET ?
                """, schemaName, where.sql(), orderBy);

        List<Object> args = new ArrayList<>(where.args());
        args.add(limit);
        args.add(offset);

        return jdbcTemplate.query(sql, staffRowMapper, args.toArray());
    }

    public Optional<TenantStaffResponseDTO> findStaffById(String schemaName, Long id) {
        validateSchemaName(schemaName);

        String sql = String.format("""
                SELECT u.id,
                       u.uuid,
                       u.title,
                       u.email,
                       u.phone_number,
                       u.status,
                       ut.c_name AS role
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                WHERE u.deleted_at IS NULL
                  AND u.id = ?
                LIMIT 1
                """, schemaName);

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) return Optional.of(staffRowMapper.mapRow(rs, 0));
            return Optional.empty();
        }, id);
    }

    public long countStaff(String schemaName, String role, Integer status, String name) {
        validateSchemaName(schemaName);
        SqlAndArgs where = buildWhere(role, status, name);

        String sql = String.format("""
                SELECT COUNT(1)
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                WHERE u.deleted_at IS NULL
                  %s
                """, schemaName, where.sql());

        Long total = jdbcTemplate.queryForObject(sql, Long.class, where.args().toArray());
        return total == null ? 0 : total;
    }

    public List<RoleCountDTO> countByRole(String schemaName, Integer status, String name) {
        validateSchemaName(schemaName);
        SqlAndArgs where = buildWhere(null, status, name);

        String sql = String.format("""
                SELECT COALESCE(ut.c_name, 'UNKNOWN') AS role,
                       COUNT(1) AS cnt
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                WHERE u.deleted_at IS NULL
                  %s
                GROUP BY COALESCE(ut.c_name, 'UNKNOWN')
                ORDER BY cnt DESC, role ASC
                """, schemaName, where.sql());

        return jdbcTemplate.query(sql, (rs, rowNum) -> RoleCountDTO.builder()
                .role(rs.getString("role"))
                .count(rs.getLong("cnt"))
                .build(), where.args().toArray());
    }

    private record SqlAndArgs(String sql, List<Object> args) {}

    private SqlAndArgs buildWhere(String role, Integer status, String name) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (role != null && !role.isBlank()) {
            clauses.add("lower(ut.c_name) = ?");
            args.add(role.trim().toLowerCase(Locale.ROOT));
        }
        if (status != null) {
            clauses.add("u.status = ?");
            args.add(status);
        }
        if (name != null && !name.isBlank()) {
            clauses.add("u.title ILIKE ?");
            args.add("%" + name.trim() + "%");
        }

        if (clauses.isEmpty()) {
            return new SqlAndArgs("", List.of());
        }
        return new SqlAndArgs(" AND " + String.join(" AND ", clauses), args);
    }

    private String orderBy(String sortBy, String sortDir) {
        String dir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        String key = sortBy == null ? "" : sortBy.trim().toLowerCase(Locale.ROOT);
        String col = switch (key) {
            case "id" -> "u.id";
            case "title", "name" -> "u.title";
            case "email" -> "u.email";
            case "phone_number", "phone" -> "u.phone_number";
            case "status" -> "u.status";
            case "role" -> "ut.c_name";
            case "created_at" -> "u.created_at";
            default -> "u.id";
        };
        return "ORDER BY " + col + " " + dir;
    }
}

