package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;

@Builder
public record TenantStaffResponseDTO(
        Long id,
        String uuid,
        String title,
        String email,
        String phoneNumber,
        Integer status,
        String role
) {
}

