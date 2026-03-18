package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;
import org.arghyam.jalsoochak.user.enums.TenantUserStatus;

import java.util.List;

@Builder
public record TenantStaffResponseDTO(
        Long id,
        String uuid,
        String title,
        String email,
        String phoneNumber,
        TenantUserStatus status,
        String role,
        List<SchemeSummaryDTO> schemes
) {
}
