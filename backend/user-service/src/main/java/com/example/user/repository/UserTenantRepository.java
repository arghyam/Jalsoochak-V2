package com.example.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserTenantRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsPhoneNumber(String schemaName, String phoneNumber) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT EXISTS (
                    SELECT 1
                    FROM %s.user_table
                    WHERE phone_number = ?
                )
                """, schemaName);
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, phoneNumber);
        return Boolean.TRUE.equals(exists);
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    public Optional<TenantUserRecord> findUserByPhone(String schemaName, String phoneNumber) {
        validateSchemaName(schemaName);

        String sql = String.format("""
        SELECT u.id,
               u.phone_number,
               u.email,
               u.user_type,
               ut.c_name
        FROM %s.user_table u
        LEFT JOIN common_schema.user_type_master_table ut
               ON ut.id = u.user_type
        WHERE u.phone_number = ?
        """, schemaName);

        List<TenantUserRecord> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TenantUserRecord(
                        toLong(rs.getObject("id")),
                        rs.getString("phone_number"),
                        rs.getString("email"),
                        toLong(rs.getObject("user_type")),
                        rs.getString("c_name")
                ), phoneNumber);

        return rows.stream().findFirst();
    }

    public Optional<TenantUserRecord> findUserById(String schemaName, Long userId) {
        validateSchemaName(schemaName);

        String sql = String.format("""
        SELECT u.id,
               u.phone_number,
               u.email,
               u.user_type,
               ut.c_name
        FROM %s.user_table u
        LEFT JOIN common_schema.user_type_master_table ut
               ON ut.id = u.user_type
        WHERE u.id = ?
        """, schemaName);

        List<TenantUserRecord> rows = jdbcTemplate.query(sql, (rs, n) ->
                new TenantUserRecord(
                        toLong(rs.getObject("id")),
                        rs.getString("phone_number"),
                        rs.getString("email"),
                        toLong(rs.getObject("user_type")),
                        rs.getString("c_name")
                ), userId);

        return rows.stream().findFirst();
    }

    public boolean existsEmail(String schemaName, String email) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT EXISTS (
                    SELECT 1
                    FROM %s.user_table
                    WHERE LOWER(email) = LOWER(?)
                )
                """, schemaName);
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, email);
        return Boolean.TRUE.equals(exists);
    }

    public Long createUser(String schemaName,
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
                    tenant_id,
                    title,
                    email,
                    user_type,
                    phone_number,
                    password,
                    status,
                    email_verification_status,
                    phone_verification_status,
                    created_by,
                    created_at,
                    updated_by,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 1, true, true, ?, NOW(), ?, NOW())
                RETURNING id
                """, schemaName);

        Number insertedId = jdbcTemplate.queryForObject(
                sql,
                Number.class,
                tenantId,
                title,
                email,
                userTypeId,
                phoneNumber,
                password,
                createdBy,
                createdBy
        );
        return insertedId != null ? insertedId.longValue() : null;
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
}
