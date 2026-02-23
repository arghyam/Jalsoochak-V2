package com.example.user.repository;

public record TenantUserRecord(
        Long id,
        Integer tenantId,
        String phoneNumber,
        String email,
        Long userTypeId,
        String cName
) {}
