package org.arghyam.jalsoochak.user.service;

import org.arghyam.jalsoochak.user.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.user.dto.request.UpdateStaffRoleRequestDTO;
import org.arghyam.jalsoochak.user.dto.response.RoleCountDTO;
import org.arghyam.jalsoochak.user.dto.response.TenantStaffResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface TenantStaffService {

    PageResponseDTO<TenantStaffResponseDTO> listStaff(
            String tenantCode,
            int page,
            int limit,
            String sortBy,
            String sortDir,
            List<String> role,
            String status,
            String name
    );

    List<RoleCountDTO> countStaffByRole(String tenantCode, String status, String name);

    TenantStaffResponseDTO updateStaffRole(Long id, UpdateStaffRoleRequestDTO request, Authentication caller);
}
