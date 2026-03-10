package org.arghyam.jalsoochak.user.repository.records;

import java.time.LocalDateTime;

public record AdminUserRow(
        Long id,
        String uuid,
        String email,
        String phoneNumber,
        Integer tenantId,
        Integer adminLevel,
        Integer status,
        Integer createdBy,
        LocalDateTime createdAt
) {}
