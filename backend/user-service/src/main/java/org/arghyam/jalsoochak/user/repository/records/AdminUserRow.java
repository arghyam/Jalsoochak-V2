package org.arghyam.jalsoochak.user.repository.records;

import org.arghyam.jalsoochak.user.enums.AdminUserStatus;

import java.time.LocalDateTime;

public record AdminUserRow(
        Long id,
        String uuid,
        String email,
        String phoneNumber,
        Integer tenantId,
        Integer adminLevel,
        AdminUserStatus status,
        Integer createdBy,
        LocalDateTime createdAt
) {}
