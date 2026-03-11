package org.arghyam.jalsoochak.user.repository.records;

import java.time.Instant;

public record AdminUserTokenRow(
        Long id,
        String email,
        String tokenHash,
        String tokenType,
        String metadata,       // raw JSONB string; service parses as needed
        Instant expiresAt,
        Instant usedAt,        // null = still active
        Instant deletedAt,     // null = not revoked
        Instant createdAt
) {}
