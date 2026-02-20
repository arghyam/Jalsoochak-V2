package com.example.user.repository;

import java.time.LocalDateTime;

public record InviteTokenRow(
        Long id,
        String email,
        String token,
        LocalDateTime expiresAt,
        Long senderId,
        boolean used
) {
}
