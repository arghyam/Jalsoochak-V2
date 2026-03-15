package org.arghyam.jalsoochak.user.repository;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.enums.AdminUserStatus;
import org.arghyam.jalsoochak.user.exceptions.BadRequestException;
import org.arghyam.jalsoochak.user.repository.records.AdminUserRow;
import org.arghyam.jalsoochak.user.repository.records.AdminUserTokenRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserCommonRepository {

    private final JdbcTemplate jdbcTemplate;

    // --- Tenant lookups ---

    public boolean existsTenantByStateCode(String stateCode) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM common_schema.tenant_master_table
                    WHERE LOWER(state_code) = LOWER(?)
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, stateCode);
        return Boolean.TRUE.equals(exists);
    }

    public Optional<Integer> findTenantIdByStateCode(String stateCode) {
        String sql = """
                SELECT id
                FROM common_schema.tenant_master_table
                WHERE LOWER(state_code) = LOWER(?)
                LIMIT 1
                """;
        List<Integer> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getInt("id"), stateCode);
        return rows.stream().findFirst();
    }

    public Optional<String> findTenantStateCodeById(Integer tenantId) {
        String sql = """
                SELECT state_code
                FROM common_schema.tenant_master_table
                WHERE id = ?
                LIMIT 1
                """;
        List<String> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getString("state_code"), tenantId);
        return rows.stream().findFirst();
    }

    public Optional<String> findTenantTitleByStateCode(String stateCode) {
        String sql = """
                SELECT title
                FROM common_schema.tenant_master_table
                WHERE LOWER(state_code) = LOWER(?)
                LIMIT 1
                """;
        List<String> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getString("title"), stateCode);
        return rows.stream().findFirst();
    }

    // --- User type lookups ---

    public Optional<Integer> findUserTypeIdByName(String cName) {
        String sql = """
                SELECT id
                FROM common_schema.user_type_master_table
                WHERE LOWER(c_name) = LOWER(?)
                LIMIT 1
                """;
        List<Integer> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getInt("id"), cName);
        return rows.stream().findFirst();
    }

    public Optional<String> findUserTypeNameById(Integer id) {
        String sql = """
                SELECT c_name
                FROM common_schema.user_type_master_table
                WHERE id = ?
                LIMIT 1
                """;
        List<String> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getString("c_name"), id);
        return rows.stream().findFirst();
    }

    // --- Admin user CRUD ---

    public Long createAdminUser(String uuid, String email, String phoneNumber,
                                Integer tenantId, Integer adminLevelId, Integer createdById) {
        String sql = """
                INSERT INTO common_schema.tenant_admin_user_master_table
                    (uuid, email, phone_number, tenant_id, admin_level, password, status, created_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'KEYCLOAK_MANAGED', 1, ?, NOW(), NOW())
                RETURNING id
                """;
        Number id = jdbcTemplate.queryForObject(sql, Number.class,
                uuid, email, phoneNumber, tenantId, adminLevelId, createdById);
        return id != null ? id.longValue() : null;
    }

    /**
     * Create a PENDING admin user at invite time (before Keycloak account exists).
     * status=2 (PENDING). UUID is a placeholder; replaced at activation.
     */
    public Long createAdminUserPending(String email, Integer tenantId, Integer adminLevelId, Integer createdById) {
        String sql = """
                INSERT INTO common_schema.tenant_admin_user_master_table
                    (uuid, email, phone_number, tenant_id, admin_level, password, status, created_by, created_at, updated_at)
                VALUES (gen_random_uuid()::TEXT, ?, '', ?, ?, 'KEYCLOAK_MANAGED', 2, ?, NOW(), NOW())
                RETURNING id
                """;
        Number id = jdbcTemplate.queryForObject(sql, Number.class,
                email, tenantId, adminLevelId, createdById);
        return id != null ? id.longValue() : null;
    }

    /**
     * Complete activation of a PENDING admin user after Keycloak account creation.
     * Sets the real Keycloak UUID, phone number, and status = 1 (active).
     */
    public void activatePendingAdminUser(Long id, String keycloakUuid, String phoneNumber) {
        int updated = jdbcTemplate.update("""
                UPDATE common_schema.tenant_admin_user_master_table
                SET uuid = ?, phone_number = ?, status = 1, updated_at = NOW()
                WHERE id = ? AND status = 2 AND deleted_at IS NULL
                """, keycloakUuid, phoneNumber, id);
        if (updated != 1) {
            throw new IllegalStateException("Expected to activate exactly one pending user with id=" + id + " but updated " + updated + " rows");
        }
    }

    public Optional<AdminUserRow> findAdminUserByUuid(String uuid) {
        String sql = """
                SELECT id, uuid, email, phone_number, tenant_id, admin_level, status, created_by, created_at
                FROM common_schema.tenant_admin_user_master_table
                WHERE uuid = ?
                LIMIT 1
                """;
        List<AdminUserRow> rows = jdbcTemplate.query(sql, (rs, n) -> mapAdminUserRow(rs), uuid);
        return rows.stream().findFirst();
    }

    public Optional<AdminUserRow> findAdminUserByEmail(String email) {
        String sql = """
                SELECT id, uuid, email, phone_number, tenant_id, admin_level, status, created_by, created_at
                FROM common_schema.tenant_admin_user_master_table
                WHERE LOWER(email) = LOWER(?)
                LIMIT 1
                """;
        List<AdminUserRow> rows = jdbcTemplate.query(sql, (rs, n) -> mapAdminUserRow(rs), email);
        return rows.stream().findFirst();
    }

    public Optional<AdminUserRow> findAdminUserById(Long id) {
        String sql = """
                SELECT id, uuid, email, phone_number, tenant_id, admin_level, status, created_by, created_at
                FROM common_schema.tenant_admin_user_master_table
                WHERE id = ?
                LIMIT 1
                """;
        List<AdminUserRow> rows = jdbcTemplate.query(sql, (rs, n) -> mapAdminUserRow(rs), id);
        return rows.stream().findFirst();
    }

    public boolean existsAdminUserByEmail(String email) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM common_schema.tenant_admin_user_master_table
                    WHERE LOWER(email) = LOWER(?)
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, email);
        return Boolean.TRUE.equals(exists);
    }

    /** True only if a non-PENDING (active or deactivated) user exists with this email. */
    public boolean existsActiveAdminUserByEmail(String email) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM common_schema.tenant_admin_user_master_table
                    WHERE LOWER(email) = LOWER(?) AND status != 2
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, email);
        return Boolean.TRUE.equals(exists);
    }

    public void updateAdminUserProfile(Long id, String phoneNumber, Long updatedById) {
        String sql = """
                UPDATE common_schema.tenant_admin_user_master_table
                SET phone_number = ?, updated_by = ?, updated_at = NOW()
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, phoneNumber, updatedById, id);
    }

    public void deactivateAdminUser(Long id, Long actorId) {
        String sql = """
                UPDATE common_schema.tenant_admin_user_master_table
                SET status = 0, deleted_at = NOW(), deleted_by = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, actorId, id);
    }

    public void activateAdminUser(Long id, Long actorId) {
        String sql = """
                UPDATE common_schema.tenant_admin_user_master_table
                SET status = 1, deleted_at = NULL, deleted_by = NULL, updated_by = ?, updated_at = NOW()
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, actorId, id);
    }

    // --- Counting for minimum-active guards ---

    public int countActiveSuperUsers() {
        String sql = """
                SELECT COUNT(*)
                FROM common_schema.tenant_admin_user_master_table
                WHERE tenant_id = 0 AND status = 1 AND deleted_at IS NULL
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    public int countActiveStateAdminsForTenant(Integer tenantId) {
        String sql = """
                SELECT COUNT(*)
                FROM common_schema.tenant_admin_user_master_table
                WHERE tenant_id = ? AND status = 1 AND deleted_at IS NULL
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
        return count != null ? count : 0;
    }

    // --- Listing (paginated) ---

    public List<AdminUserRow> listSuperUsers(long offset, int limit) {
        if (limit <= 0) {
            throw new BadRequestException("limit must be greater than 0");
        }
        if (offset < 0) {
            throw new BadRequestException("offset must be non-negative");
        }
        String sql = """
                SELECT id, uuid, email, phone_number, tenant_id, admin_level, status, created_by, created_at
                FROM common_schema.tenant_admin_user_master_table
                WHERE tenant_id = 0
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;
        return jdbcTemplate.query(sql, (rs, n) -> mapAdminUserRow(rs), limit, offset);
    }

    public long countSuperUsers() {
        String sql = """
                SELECT COUNT(*)
                FROM common_schema.tenant_admin_user_master_table
                WHERE tenant_id = 0
                """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    public List<AdminUserRow> listStateAdminsByTenant(Integer tenantId, long offset, int limit) {
        if (limit <= 0) {
            throw new BadRequestException("limit must be greater than 0");
        }
        if (offset < 0) {
            throw new BadRequestException("offset must be non-negative");
        }
        if (tenantId == null) {
            String sql = """
                    SELECT id, uuid, email, phone_number, tenant_id, admin_level, status, created_by, created_at
                    FROM common_schema.tenant_admin_user_master_table
                    WHERE tenant_id != 0
                    ORDER BY created_at DESC
                    LIMIT ? OFFSET ?
                    """;
            return jdbcTemplate.query(sql, (rs, n) -> mapAdminUserRow(rs), limit, offset);
        } else {
            String sql = """
                    SELECT id, uuid, email, phone_number, tenant_id, admin_level, status, created_by, created_at
                    FROM common_schema.tenant_admin_user_master_table
                    WHERE tenant_id = ?
                    ORDER BY created_at DESC
                    LIMIT ? OFFSET ?
                    """;
            return jdbcTemplate.query(sql, (rs, n) -> mapAdminUserRow(rs), tenantId, limit, offset);
        }
    }

    public long countStateAdminsByTenant(Integer tenantId) {
        if (tenantId == null) {
            String sql = """
                    SELECT COUNT(*)
                    FROM common_schema.tenant_admin_user_master_table
                    WHERE tenant_id != 0
                    """;
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0L;
        } else {
            String sql = """
                    SELECT COUNT(*)
                    FROM common_schema.tenant_admin_user_master_table
                    WHERE tenant_id = ?
                    """;
            Long count = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
            return count != null ? count : 0L;
        }
    }

    // --- Token methods ---

    /**
     * Soft-revoke any existing active token for (email, tokenType), then insert new one.
     * Must be called from a @Transactional context.
     */
    @Transactional
    public void upsertToken(String email, String tokenHash, String tokenType,
                            String metadataJson, Instant expiresAt, Integer createdBy) {
        // Use LOWER() on both sides so the revoke matches regardless of stored case
        jdbcTemplate.update("""
                UPDATE common_schema.admin_user_token_table
                SET deleted_at = NOW()
                WHERE LOWER(email) = LOWER(?) AND token_type = ? AND used_at IS NULL AND deleted_at IS NULL
                """, email, tokenType);
        jdbcTemplate.update("""
                INSERT INTO common_schema.admin_user_token_table
                    (email, token_hash, token_type, metadata, expires_at, created_by)
                VALUES (?, ?, ?, ?::jsonb, ?, ?)
                """, email.toLowerCase(), tokenHash, tokenType, metadataJson,
                Timestamp.from(expiresAt), createdBy);
    }

    /** Find an active (not used, not deleted, not expired) token by hash. */
    public Optional<AdminUserTokenRow> findActiveTokenByHash(String tokenHash) {
        String sql = """
                SELECT id, email, token_hash, token_type, metadata::TEXT, expires_at, used_at, deleted_at, created_at
                FROM common_schema.admin_user_token_table
                WHERE token_hash = ? AND used_at IS NULL AND deleted_at IS NULL AND expires_at > NOW()
                LIMIT 1
                """;
        List<AdminUserTokenRow> rows = jdbcTemplate.query(sql, (rs, n) -> mapTokenRow(rs), tokenHash);
        return rows.stream().findFirst();
    }

    /**
     * Atomically validates and consumes an active, non-expired token in a single UPDATE.
     * Returns the consumed row if successful; empty if the token is invalid, expired, or already used.
     */
    @Transactional
    public Optional<AdminUserTokenRow> consumeActiveToken(String tokenHash) {
        String sql = """
                UPDATE common_schema.admin_user_token_table
                SET used_at = NOW()
                WHERE token_hash = ? AND used_at IS NULL AND deleted_at IS NULL AND expires_at > NOW()
                RETURNING id, email, token_hash, token_type, metadata::TEXT, expires_at, used_at, deleted_at, created_at
                """;
        List<AdminUserTokenRow> rows = jdbcTemplate.query(sql, (rs, n) -> mapTokenRow(rs), tokenHash);
        return rows.stream().findFirst();
    }

    /**
     * Atomically validates and consumes an active, non-expired token only when its type matches.
     * Returns the consumed row if successful; empty if invalid, expired, used, or type mismatch.
     */
    @Transactional
    public Optional<AdminUserTokenRow> consumeActiveTokenOfType(String tokenHash, String expectedType) {
        String sql = """
                UPDATE common_schema.admin_user_token_table
                SET used_at = NOW()
                WHERE token_hash = ? AND token_type = ? AND used_at IS NULL AND deleted_at IS NULL AND expires_at > NOW()
                RETURNING id, email, token_hash, token_type, metadata::TEXT, expires_at, used_at, deleted_at, created_at
                """;
        List<AdminUserTokenRow> rows = jdbcTemplate.query(sql, (rs, n) -> mapTokenRow(rs), tokenHash, expectedType);
        return rows.stream().findFirst();
    }

    /** Admin revocation. */
    public void revokeToken(String tokenHash, Long revokedById) {
        jdbcTemplate.update("""
                UPDATE common_schema.admin_user_token_table
                SET deleted_at = NOW(), deleted_by = ?
                WHERE token_hash = ?
                """, revokedById, tokenHash);
    }

    // --- Row mappers ---

    private AdminUserRow mapAdminUserRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        return new AdminUserRow(
                rs.getLong("id"),
                rs.getString("uuid"),
                rs.getString("email"),
                rs.getString("phone_number"),
                rs.getInt("tenant_id"),
                rs.getInt("admin_level"),
                AdminUserStatus.fromCode(rs.getInt("status")),
                rs.getInt("created_by"),
                createdAtTs != null ? createdAtTs.toLocalDateTime() : null
        );
    }

    private AdminUserTokenRow mapTokenRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp expiresAtTs  = rs.getTimestamp("expires_at");
        Timestamp usedAtTs     = rs.getTimestamp("used_at");
        Timestamp deletedAtTs  = rs.getTimestamp("deleted_at");
        Timestamp createdAtTs  = rs.getTimestamp("created_at");
        return new AdminUserTokenRow(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("token_hash"),
                rs.getString("token_type"),
                rs.getString("metadata"),
                expiresAtTs  != null ? expiresAtTs.toInstant()  : null,
                usedAtTs     != null ? usedAtTs.toInstant()     : null,
                deletedAtTs  != null ? deletedAtTs.toInstant()  : null,
                createdAtTs  != null ? createdAtTs.toInstant()  : null
        );
    }
}
