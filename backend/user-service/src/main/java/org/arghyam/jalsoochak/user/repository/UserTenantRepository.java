package org.arghyam.jalsoochak.user.repository;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.service.PiiEncryptionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import org.arghyam.jalsoochak.user.enums.TenantUserStatus;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserTenantRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PiiEncryptionService pii;

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    public Optional<TenantUserRecord> findUserById(String schemaName, Long userId) {
        validateSchemaName(schemaName);

        String sql = String.format("""
        SELECT u.id,
               u.tenant_id,
               u.phone_number,
               u.email,
               u.user_type,
               u.title,
               u.uuid,
               u.status,
               u.whatsapp_connection_id,
               ut.c_name
        FROM %s.user_table u
        LEFT JOIN common_schema.user_type_master_table ut
               ON ut.id = u.user_type
        WHERE u.id = ?
        """, schemaName);

        List<TenantUserRecord> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TenantUserRecord(
                        toLong(rs.getObject("id")),
                        toInteger(rs.getObject("tenant_id")),
                        pii.decrypt(rs.getString("phone_number")),
                        rs.getString("email"),
                        toLong(rs.getObject("user_type")),
                        rs.getString("c_name"),
                        pii.decrypt(rs.getString("title")),
                        rs.getString("uuid"),
                        toInteger(rs.getObject("status")),
                        toLong(rs.getObject("whatsapp_connection_id"))
                ), userId);

        return rows.stream().findFirst();
    }

    public Optional<TenantUserRecord> findUserByEmail(String schemaName, String email) {
        validateSchemaName(schemaName);

        String sql = String.format("""
        SELECT u.id,
               u.tenant_id,
               u.phone_number,
               u.email,
               u.user_type,
               u.title,
               u.uuid,
               u.status,
               u.whatsapp_connection_id,
               ut.c_name
        FROM %s.user_table u
        LEFT JOIN common_schema.user_type_master_table ut
               ON ut.id = u.user_type
        WHERE LOWER(u.email) = LOWER(?)
        """, schemaName);

        List<TenantUserRecord> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TenantUserRecord(
                        toLong(rs.getObject("id")),
                        toInteger(rs.getObject("tenant_id")),
                        pii.decrypt(rs.getString("phone_number")),
                        rs.getString("email"),
                        toLong(rs.getObject("user_type")),
                        rs.getString("c_name"),
                        pii.decrypt(rs.getString("title")),
                        rs.getString("uuid"),
                        toInteger(rs.getObject("status")),
                        toLong(rs.getObject("whatsapp_connection_id"))
                ), email);

        return rows.stream().findFirst();
    }

    public Optional<TenantUserRecord> findUserByPhone(String schemaName, String phoneNumber) {
        validateSchemaName(schemaName);
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return Optional.empty();
        }

        // Lookup via HMAC hash — the encrypted column cannot be searched directly
        String sql = String.format("""
        SELECT u.id,
               u.tenant_id,
               u.phone_number,
               u.email,
               u.user_type,
               u.title,
               u.uuid,
               u.status,
               u.whatsapp_connection_id,
               ut.c_name
        FROM %s.user_table u
        LEFT JOIN common_schema.user_type_master_table ut
               ON ut.id = u.user_type
        WHERE u.phone_number_hash = ?
        """, schemaName);

        List<TenantUserRecord> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TenantUserRecord(
                        toLong(rs.getObject("id")),
                        toInteger(rs.getObject("tenant_id")),
                        pii.decrypt(rs.getString("phone_number")),
                        rs.getString("email"),
                        toLong(rs.getObject("user_type")),
                        rs.getString("c_name"),
                        pii.decrypt(rs.getString("title")),
                        rs.getString("uuid"),
                        toInteger(rs.getObject("status")),
                        toLong(rs.getObject("whatsapp_connection_id"))
                ), pii.hmac(phoneNumber.trim()));

        return rows.stream().findFirst();
    }

    public Long createUser(String schemaName,
                           String uuid,
                           Integer tenantId,
                           String title,
                           String email,
                           Integer userTypeId,
                           String phoneNumber,
                           String password,
                           Long createdBy) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                INSERT INTO %s.user_table
                (
                    uuid,
                    tenant_id,
                    title,
                    email,
                    user_type,
                    phone_number,
                    phone_number_hash,
                    password,
                    status,
                    email_verification_status,
                    phone_verification_status,
                    created_by,
                    created_at,
                    updated_by,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, %d, true, true, ?, NOW(), ?, NOW())
                RETURNING id
                """, schemaName, TenantUserStatus.ACTIVE.code);

        Number insertedId = jdbcTemplate.queryForObject(
                sql,
                Number.class,
                uuid,
                tenantId,
                pii.encrypt(title),
                email,
                userTypeId,
                pii.encrypt(phoneNumber),
                pii.hmac(phoneNumber),
                password,
                createdBy,
                createdBy
        );
        return insertedId != null ? insertedId.longValue() : null;
    }

    public void updateUserProfile(String schemaName, Long id, String title, String phoneNumber) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                UPDATE %s.user_table
                SET title = ?, phone_number = ?, phone_number_hash = ?, updated_at = NOW()
                WHERE id = ?
                """, schemaName);
        jdbcTemplate.update(sql, pii.encrypt(title), pii.encrypt(phoneNumber), pii.hmac(phoneNumber), id);
    }

    public int updateUserRole(String schemaName, Long userId, Long newUserTypeId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                UPDATE %s.user_table
                SET user_type = ?, updated_at = NOW()
                WHERE id = ?
                """, schemaName);
        return jdbcTemplate.update(sql, newUserTypeId, userId);
    }

    public void updateUserLanguageId(String schemaName, Long userId, Integer languageId) {
        validateSchemaName(schemaName);
        if (userId == null) {
            return;
        }
        String sql = String.format("""
                UPDATE %s.user_table
                SET language_id = ?, updated_at = NOW()
                WHERE id = ?
                """, schemaName);
        jdbcTemplate.update(sql, languageId, userId);
    }

    /**
     * Sets the Keycloak UUID and stores an AES-encrypted managed password for a staff user.
     * Called by {@code StaffKeycloakService} on first successful OTP login (lazy provisioning).
     */
    public void updateKeycloakUuidAndPassword(String schemaName, Long userId,
                                              String keycloakUuid, String encryptedPassword) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                UPDATE %s.user_table
                SET uuid = ?, password = ?, updated_at = NOW()
                WHERE id = ?
                """, schemaName);
        jdbcTemplate.update(sql, keycloakUuid, encryptedPassword, userId);
    }

    /**
     * Returns the raw {@code password} column value for a given user.
     * Used by {@code StaffKeycloakService} to determine whether a managed password
     * has already been set (non-placeholder value → decrypt with {@code PasswordCipher}).
     */
    public Optional<String> findPasswordByUserId(String schemaName, Long userId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT password
                FROM %s.user_table
                WHERE id = ?
                LIMIT 1
                """, schemaName);
        List<String> rows = jdbcTemplate.query(sql, (rs, n) -> rs.getString("password"), userId);
        return rows.stream().findFirst();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Expected numeric DB value, got: " + value.getClass().getName());
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Expected numeric DB value, got: " + value.getClass().getName());
    }
}
