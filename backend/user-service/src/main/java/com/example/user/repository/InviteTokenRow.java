package com.example.user.repository;

import java.time.LocalDateTime;

public record InviteTokenRow(
        Long id,
        String email,
        LocalDateTime expiresAt,
        Integer tenantId,
        Long senderId,
        boolean used
) {
}
