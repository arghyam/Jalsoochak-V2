package com.example.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserCommonRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsTenantByStateCode(String tenantCode) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM common_schema.tenant_master_table
                    WHERE LOWER(state_code) = LOWER(?)
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, tenantCode);
        return Boolean.TRUE.equals(exists);
    }

    public Optional<Integer> findTenantIdByStateCode(String tenantCode) {
        String sql = """
                SELECT id
                FROM common_schema.tenant_master_table
                WHERE LOWER(state_code) = LOWER(?)
                LIMIT 1
                """;
        List<Integer> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getInt("id"), tenantCode);
        return rows.stream().findFirst();
    }

    public Optional<Integer> findUserTypeId(String personType) {
        String sql = """
                SELECT id
                FROM common_schema.user_type_master_table
                WHERE LOWER(c_name) = LOWER(?)
                   OR LOWER(c_name) = LOWER(REPLACE(?, ' ', '_'))
                LIMIT 1
                """;
        List<Integer> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getInt("id"), personType, personType);
        return rows.stream().findFirst();
    }

    public boolean existsActiveInviteByEmail(String email, Integer tenantId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM common_schema.invite_tokens
                    WHERE LOWER(email) = LOWER(?)
                      AND tenant_id = ?
                      AND used = false
                      AND expires_at > NOW()
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, email, tenantId);
        return Boolean.TRUE.equals(exists);
    }

    public void createInviteToken(String email, String rawToken, LocalDateTime expiresAt, Integer tenantId, Long senderId) {
        String tokenHash = hashToken(rawToken);
        String sql = """
                INSERT INTO common_schema.invite_tokens (email, token, token_hash, expires_at, tenant_id, sender_id, used)
                VALUES (?, NULL, ?, ?, ?, ?, false)
                """;
        jdbcTemplate.update(sql, email, tokenHash, expiresAt, tenantId, senderId);
    }

    public Optional<InviteTokenRow> findInviteTokenByToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        String sql = """
                SELECT id, email, expires_at, tenant_id, sender_id, used
                FROM common_schema.invite_tokens
                WHERE token_hash = ? OR token = ?
                LIMIT 1
                """;
        List<InviteTokenRow> rows = jdbcTemplate.query(sql, (rs, n) ->
                {
                    Timestamp expiresAtTs = rs.getTimestamp("expires_at");
                    if (expiresAtTs == null) {
                        throw new IllegalStateException("Invite token row has null expires_at");
                    }
                    return new InviteTokenRow(
                            rs.getLong("id"),
                            rs.getString("email"),
                            expiresAtTs.toLocalDateTime(),
                            rs.getInt("tenant_id"),
                            rs.getLong("sender_id"),
                            rs.getBoolean("used")
                    );
                }, tokenHash, rawToken);
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

    public void markInviteTokenUsed(Long id) {
        String sql = """
                UPDATE common_schema.invite_tokens
                SET used = true
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, id);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
