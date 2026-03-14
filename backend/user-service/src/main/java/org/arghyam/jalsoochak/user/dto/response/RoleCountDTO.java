package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;

@Builder
public record RoleCountDTO(
        String role,
        long count
) {
}

