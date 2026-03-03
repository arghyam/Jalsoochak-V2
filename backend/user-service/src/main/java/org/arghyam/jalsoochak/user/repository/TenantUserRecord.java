package org.arghyam.jalsoochak.user.repository;

public record TenantUserRecord(
        Long id,
        Integer tenantId,
        String phoneNumber,
        String email,
        Long userTypeId,
        String cName
) {}
