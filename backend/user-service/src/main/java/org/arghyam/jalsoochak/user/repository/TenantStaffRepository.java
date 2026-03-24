package org.arghyam.jalsoochak.user.repository;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.dto.response.RoleCountDTO;
import org.arghyam.jalsoochak.user.dto.response.SchemeSummaryDTO;
import org.arghyam.jalsoochak.user.dto.response.TenantStaffResponseDTO;
import org.arghyam.jalsoochak.user.enums.TenantUserStatus;
import org.arghyam.jalsoochak.user.service.PiiEncryptionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TenantStaffRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PiiEncryptionService pii;

    private static final Map<Integer, String> WORK_STATUS_LABELS = Map.of(
            1, "Ongoing",
            2, "Completed",
            3, "Not Started",
            4, "Handed Over"
    );
    private static final Map<Integer, String> OPERATING_STATUS_LABELS = Map.of(
            1, "Operative",
            2, "Non-Operative",
            3, "Partially Operative"
    );

    private RowMapper<TenantStaffResponseDTO> staffRowMapper() {
        return (rs, rowNum) -> TenantStaffResponseDTO.builder()
                .id(rs.getLong("id"))
                .uuid(rs.getString("uuid"))
                .title(safeDecrypt(rs.getString("title")))
                .email(rs.getString("email"))
                .phoneNumber(safeDecrypt(rs.getString("phone_number")))
                .status(mapStatus(rs.getObject("status")))
                .role(rs.getString("role"))
                .schemes(null)
                .build();
    }

    /** Decrypts a PII value, falling back to the raw value for legacy plaintext rows. */
    private String safeDecrypt(String value) {
        if (value == null) return null;
        try {
            return pii.decrypt(value);
        } catch (Exception e) {
            return value;
        }
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    private boolean tableExists(String schemaName, String tableName) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = ?
                      AND table_name = ?
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, schemaName, tableName);
        return Boolean.TRUE.equals(exists);
    }

    public List<TenantStaffResponseDTO> listStaff(
            String schemaName,
            List<String> roles,
            Integer status,
            String name,
            String sortBy,
            String sortDir,
            int offset,
            int limit
    ) {
        validateSchemaName(schemaName);

        // name filter cannot be applied in SQL because title is encrypted;
        // fetch all rows matching the other filters, decrypt, then filter+paginate in Java.
        boolean hasNameFilter = name != null && !name.isBlank();
        SqlAndArgs where = buildWhere(roles, status);
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
                %s
                """, schemaName, where.sql(), orderBy,
                hasNameFilter ? "" : "LIMIT ? OFFSET ?");

        List<Object> args = new ArrayList<>(where.args());
        if (!hasNameFilter) {
            args.add(limit);
            args.add(offset);
        }

        List<TenantStaffResponseDTO> rows = jdbcTemplate.query(sql, staffRowMapper(), args.toArray());

        if (hasNameFilter) {
            String needle = name.trim().toLowerCase(Locale.ROOT);
            rows = rows.stream()
                    .filter(r -> r.title() != null && r.title().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
            int toIdx = Math.min(offset + limit, rows.size());
            rows = offset >= rows.size() ? List.of() : new ArrayList<>(rows.subList(offset, toIdx));
        }

        attachSchemes(schemaName, rows);
        return rows;
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

        Optional<TenantStaffResponseDTO> result = jdbcTemplate.query(sql, rs -> {
            if (rs.next()) return Optional.of(staffRowMapper().mapRow(rs, 0));
            return Optional.empty();
        }, id);
        result.ifPresent(r -> attachSchemes(schemaName, List.of(r)));
        return result;
    }

    public long countStaff(String schemaName, List<String> roles, Integer status, String name) {
        validateSchemaName(schemaName);

        boolean hasNameFilter = name != null && !name.isBlank();

        if (hasNameFilter) {
            // Must decrypt and filter in Java — reuse the full fetch without pagination
            SqlAndArgs where = buildWhere(roles, status);
            String sql = String.format("""
                    SELECT u.title
                    FROM %s.user_table u
                    LEFT JOIN common_schema.user_type_master_table ut
                      ON ut.id = u.user_type
                    WHERE u.deleted_at IS NULL
                      %s
                    """, schemaName, where.sql());
            String needle = name.trim().toLowerCase(Locale.ROOT);
            List<String> encryptedTitles = jdbcTemplate.query(
                    sql, (rs, n) -> rs.getString("title"), where.args().toArray());
            return encryptedTitles.stream()
                    .map(this::safeDecrypt)
                    .filter(t -> t != null && t.toLowerCase(Locale.ROOT).contains(needle))
                    .count();
        }

        SqlAndArgs where = buildWhere(roles, status);
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
        boolean hasNameFilter = name != null && !name.isBlank();
        SqlAndArgs where = buildWhere(List.of(), status);

        if (hasNameFilter) {
            // title is encrypted — fetch all rows, decrypt, filter by name, then count by role
            String sql = String.format("""
                    SELECT u.title,
                           COALESCE(ut.c_name, 'UNKNOWN') AS role
                    FROM %s.user_table u
                    LEFT JOIN common_schema.user_type_master_table ut
                      ON ut.id = u.user_type
                    WHERE u.deleted_at IS NULL
                      %s
                    """, schemaName, where.sql());
            String needle = name.trim().toLowerCase(Locale.ROOT);
            record TitleRole(String title, String role) {}
            List<TitleRole> all = jdbcTemplate.query(sql,
                    (rs, n) -> new TitleRole(rs.getString("title"), rs.getString("role")),
                    where.args().toArray());
            Map<String, Long> roleCounts = new HashMap<>();
            for (TitleRole tr : all) {
                String decTitle = safeDecrypt(tr.title());
                if (decTitle != null && decTitle.toLowerCase(Locale.ROOT).contains(needle)) {
                    roleCounts.merge(tr.role(), 1L, Long::sum);
                }
            }
            return roleCounts.entrySet().stream()
                    .map(e -> RoleCountDTO.builder().role(e.getKey()).count(e.getValue()).build())
                    .sorted(Comparator.comparingLong(RoleCountDTO::count).reversed()
                            .thenComparing(RoleCountDTO::role))
                    .collect(Collectors.toList());
        }

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

    /** Combines paginated items and total count so callers avoid a second DB scan. */
    public record StaffPage(List<TenantStaffResponseDTO> items, long total) {}

    /**
     * Returns a page of staff and the total count in one call.
     * When a name filter is present the full result set is materialised, decrypted,
     * and filtered once in Java — avoiding the separate {@link #listStaff} +
     * {@link #countStaff} double-scan that would otherwise occur.
     */
    public StaffPage listStaffPage(
            String schemaName,
            List<String> roles,
            Integer status,
            String name,
            String sortBy,
            String sortDir,
            int offset,
            int limit
    ) {
        validateSchemaName(schemaName);
        boolean hasNameFilter = name != null && !name.isBlank();
        SqlAndArgs where = buildWhere(roles, status);
        String orderBy = orderBy(sortBy, sortDir);

        String baseSql = String.format("""
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
                """, schemaName, where.sql(), orderBy);

        if (hasNameFilter) {
            // Single DB scan; pagination and count both derived from the filtered list
            List<Object> args = new ArrayList<>(where.args());
            List<TenantStaffResponseDTO> allRows = jdbcTemplate.query(baseSql, staffRowMapper(), args.toArray());
            String needle = name.trim().toLowerCase(Locale.ROOT);
            List<TenantStaffResponseDTO> filteredRows = allRows.stream()
                    .filter(r -> r.title() != null && r.title().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
            long total = filteredRows.size();
            int toIdx = Math.min(offset + limit, filteredRows.size());
            List<TenantStaffResponseDTO> page = offset >= filteredRows.size()
                    ? List.of()
                    : new ArrayList<>(filteredRows.subList(offset, toIdx));
            attachSchemes(schemaName, page);
            return new StaffPage(page, total);
        }

        // No name filter: DB-level pagination + a lightweight count query
        String pagedSql = baseSql + "LIMIT ? OFFSET ?";
        List<Object> pagedArgs = new ArrayList<>(where.args());
        pagedArgs.add(limit);
        pagedArgs.add(offset);
        List<TenantStaffResponseDTO> items = jdbcTemplate.query(pagedSql, staffRowMapper(), pagedArgs.toArray());

        String countSql = String.format("""
                SELECT COUNT(1)
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                WHERE u.deleted_at IS NULL
                  %s
                """, schemaName, where.sql());
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, where.args().toArray());

        attachSchemes(schemaName, items);
        return new StaffPage(items, total == null ? 0 : total);
    }

    private record SqlAndArgs(String sql, List<Object> args) {}

    // name is excluded from SQL filtering because title is encrypted
    private SqlAndArgs buildWhere(List<String> roles, Integer status) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (roles != null && !roles.isEmpty()) {
            List<String> cleaned = roles.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .toList();
            if (!cleaned.isEmpty()) {
                String placeholders = String.join(", ", cleaned.stream().map(r -> "?").toList());
                clauses.add("lower(ut.c_name) IN (" + placeholders + ")");
                args.addAll(cleaned);
            }
        }
        if (status != null) {
            clauses.add("u.status = ?");
            args.add(status);
        }

        if (clauses.isEmpty()) {
            return new SqlAndArgs("", List.of());
        }
        return new SqlAndArgs(" AND " + String.join(" AND ", clauses), args);
    }

    private String orderBy(String sortBy, String sortDir) {
        String dir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        String key = sortBy == null ? "" : sortBy.trim().toLowerCase(Locale.ROOT);
        // title and phone_number are encrypted — sorting by ciphertext is meaningless,
        // fall back to u.id for consistent ordering.
        String col = switch (key) {
            case "id" -> "u.id";
            case "email" -> "u.email";
            case "status" -> "u.status";
            case "role" -> "ut.c_name";
            case "created_at" -> "u.created_at";
            default -> "u.id";
        };
        return "ORDER BY " + col + " " + dir;
    }

    private TenantUserStatus mapStatus(Object status) {
        if (status == null) {
            return null;
        }
        if (status instanceof Number num) {
            return TenantUserStatus.fromCode(num.intValue());
        }
        return TenantUserStatus.fromCode(Integer.parseInt(status.toString()));
    }

    private void attachSchemes(String schemaName, List<TenantStaffResponseDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        if (!tableExists(schemaName, "user_scheme_mapping_table")) {
            for (int i = 0; i < rows.size(); i++) {
                TenantStaffResponseDTO row = rows.get(i);
                rows.set(i, TenantStaffResponseDTO.builder()
                        .id(row.id())
                        .uuid(row.uuid())
                        .title(row.title())
                        .email(row.email())
                        .phoneNumber(row.phoneNumber())
                        .status(row.status())
                        .role(row.role())
                        .schemes(List.of())
                        .build());
            }
            return;
        }

        List<Long> userIds = rows.stream()
                .map(TenantStaffResponseDTO::id)
                .filter(Objects::nonNull)
                .toList();
        if (userIds.isEmpty()) {
            return;
        }

        String placeholders = String.join(", ", userIds.stream().map(v -> "?").toList());
        String sql = String.format("""
                SELECT usm.user_id,
                       sm.id AS scheme_id,
                       sm.scheme_name,
                       sm.work_status,
                       sm.operating_status
                FROM %s.user_scheme_mapping_table usm
                JOIN %s.scheme_master_table sm
                  ON sm.id = usm.scheme_id
                 AND sm.deleted_at IS NULL
                WHERE usm.deleted_at IS NULL
                  AND usm.status = 1
                  AND usm.user_id IN (%s)
                ORDER BY usm.user_id ASC, sm.id ASC
                """, schemaName, schemaName, placeholders);

        Map<Long, List<SchemeSummaryDTO>> byUser = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            long userId = rs.getLong("user_id");
            SchemeSummaryDTO scheme = SchemeSummaryDTO.builder()
                    .schemeId(rs.getLong("scheme_id"))
                    .schemeName(rs.getString("scheme_name"))
                    .workStatus(workStatusLabel(getNullableInt(rs.getObject("work_status"))))
                    .operatingStatus(operatingStatusLabel(getNullableInt(rs.getObject("operating_status"))))
                    .build();
            byUser.computeIfAbsent(userId, k -> new ArrayList<>()).add(scheme);
        }, userIds.toArray());

        for (int i = 0; i < rows.size(); i++) {
            TenantStaffResponseDTO row = rows.get(i);
            List<SchemeSummaryDTO> schemes = byUser.getOrDefault(row.id(), List.of());
            rows.set(i, TenantStaffResponseDTO.builder()
                    .id(row.id())
                    .uuid(row.uuid())
                    .title(row.title())
                    .email(row.email())
                    .phoneNumber(row.phoneNumber())
                    .status(row.status())
                    .role(row.role())
                    .schemes(schemes)
                    .build());
        }
    }

    private String workStatusLabel(Integer code) {
        if (code == null) {
            return "Unknown";
        }
        return WORK_STATUS_LABELS.getOrDefault(code, "Unknown");
    }

    private String operatingStatusLabel(Integer code) {
        if (code == null) {
            return "Unknown";
        }
        return OPERATING_STATUS_LABELS.getOrDefault(code, "Unknown");
    }

    private Integer getNullableInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        return Integer.valueOf(value.toString());
    }
}
