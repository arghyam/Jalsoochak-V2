package org.arghyam.jalsoochak.user.service;

import org.arghyam.jalsoochak.user.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.user.dto.response.RoleCountDTO;
import org.arghyam.jalsoochak.user.dto.response.TenantStaffResponseDTO;

import java.util.List;

public interface TenantStaffService {

    PageResponseDTO<TenantStaffResponseDTO> listStaff(
            String tenantCode,
            int page,
            int limit,
            String sortBy,
            String sortDir,
            String role,
            Integer status,
            String name
    );

    List<RoleCountDTO> countStaffByRole(String tenantCode, Integer status, String name);
}
