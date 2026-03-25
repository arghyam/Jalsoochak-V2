package org.arghyam.jalsoochak.user.repository;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.enums.OtpType;
import org.arghyam.jalsoochak.user.repository.records.OtpRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate-based repository for {@code common_schema.otp_table}.
 * All operations are intentionally simple; atomicity of multi-step flows
 * is enforced at the service/transaction level.
 */
@Repository
@RequiredArgsConstructor
public class OtpRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Marks any active (used_at IS NULL) OTP for the given user/tenant/type as consumed.
     * Called before inserting a new OTP to keep the partial unique index clean.
     */
    public void revokeActiveOtp(Long userId, Integer tenantId, OtpType otpType) {
        jdbcTemplate.update("""
                UPDATE common_schema.otp_table
                SET used_at = NOW()
                WHERE user_id = ? AND tenant_id = ? AND otp_type = ? AND used_at IS NULL
                """, userId, tenantId, otpType.name());
    }

    /**
     * Inserts a new OTP row.
     * The {@code uq_active_otp} partial unique index guarantees at most one active OTP per
     * user/tenant/type — always call {@link #revokeActiveOtp} first within the same transaction.
     */
    public void insertOtp(Long userId, Integer tenantId, OtpType otpType,
                          String encryptedOtp, Instant expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO common_schema.otp_table
                    (otp, tenant_id, user_id, otp_type, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """, encryptedOtp, tenantId, userId, otpType.name(), Timestamp.from(expiresAt));
    }

    /**
     * Returns the single active, non-expired OTP for a given user/tenant/type.
     * Active = {@code used_at IS NULL AND expires_at > NOW()}.
     */
    public Optional<OtpRow> findActiveOtp(Long userId, Integer tenantId, OtpType otpType) {
        List<OtpRow> rows = jdbcTemplate.query("""
                SELECT id, otp, tenant_id, user_id, otp_type, attempt_count, created_at, expires_at, used_at
                FROM common_schema.otp_table
                WHERE user_id = ? AND tenant_id = ? AND otp_type = ?
                  AND used_at IS NULL AND expires_at > NOW()
                LIMIT 1
                """, (rs, n) -> mapRow(rs), userId, tenantId, otpType.name());
        return rows.stream().findFirst();
    }

    /**
     * Atomically increments attempt_count by 1.
     */
    public void incrementAttemptCount(Long otpId) {
        jdbcTemplate.update("""
                UPDATE common_schema.otp_table
                SET attempt_count = attempt_count + 1
                WHERE id = ?
                """, otpId);
    }

    /**
     * Marks the OTP as consumed. Removes it from the partial unique index,
     * allowing a new OTP to be requested immediately if needed.
     */
    public void markUsed(Long otpId) {
        jdbcTemplate.update("""
                UPDATE common_schema.otp_table
                SET used_at = NOW()
                WHERE id = ?
                """, otpId);
    }

    private OtpRow mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        Timestamp usedAt    = rs.getTimestamp("used_at");
        return new OtpRow(
                rs.getLong("id"),
                rs.getString("otp"),
                rs.getInt("tenant_id"),
                rs.getInt("user_id"),
                OtpType.valueOf(rs.getString("otp_type")),
                rs.getInt("attempt_count"),
                createdAt != null ? createdAt.toInstant() : null,
                expiresAt != null ? expiresAt.toInstant() : null,
                usedAt    != null ? usedAt.toInstant()    : null
        );
    }
}
