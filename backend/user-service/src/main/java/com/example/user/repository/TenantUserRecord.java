package com.example.user.repository;

public record TenantUserRecord(
        Long id,
        String phoneNumber,
        String email,
        Long userTypeId,
        String cName
) {}