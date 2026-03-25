package org.arghyam.jalsoochak.user.repository;

/**
 * Immutable projection of a row from {@code <tenant_schema>.user_table}.
 * PII fields ({@code phoneNumber}, {@code title}) are decrypted by the row mapper
 * in {@link UserTenantRepository} before being stored here.
 */
public record TenantUserRecord(
        Long id,
        Integer tenantId,
        String phoneNumber,
        String email,
        Long userTypeId,
        String cName,
        String title,
        String keycloakUuid,
        Integer status,
        Long whatsappConnectionId
) {}
