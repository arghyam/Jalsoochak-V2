package org.arghyam.jalsoochak.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateStaffRoleRequestDTO(
        @NotBlank String tenantCode,
        @NotBlank String newRole
) {}
