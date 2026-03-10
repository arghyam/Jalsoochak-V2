package org.arghyam.jalsoochak.user.repository.records;

import java.time.LocalDateTime;

public record AdminUserTokenRow(
        Long id,
        String email,
        String tokenHash,
        String tokenType,
        String metadata,          // raw JSONB string; service parses as needed
        LocalDateTime expiresAt,
        LocalDateTime usedAt,     // null = still active
        LocalDateTime deletedAt,  // null = not revoked
        LocalDateTime createdAt
) {}
