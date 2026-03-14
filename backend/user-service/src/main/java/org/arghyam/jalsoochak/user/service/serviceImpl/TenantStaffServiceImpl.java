package org.arghyam.jalsoochak.user.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.user.dto.response.RoleCountDTO;
import org.arghyam.jalsoochak.user.dto.response.TenantStaffResponseDTO;
import org.arghyam.jalsoochak.user.repository.TenantStaffRepository;
import org.arghyam.jalsoochak.user.service.TenantStaffService;
import org.springframework.stereotype.Service;
import org.arghyam.jalsoochak.user.util.TenantSchemaResolver;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantStaffServiceImpl implements TenantStaffService {

    private final TenantStaffRepository tenantStaffRepository;

    @Override
    public PageResponseDTO<TenantStaffResponseDTO> listStaff(
            String tenantCode,
            int page,
            int limit,
            String sortBy,
            String sortDir,
            String role,
            Integer status,
            String name
    ) {
        String schemaName = TenantSchemaResolver.requireSchemaNameFromTenantCode(tenantCode);

        int p = Math.max(0, page);
        int size = clampLimit(limit);
        int offset = p * size;

        List<TenantStaffResponseDTO> rows = tenantStaffRepository.listStaff(schemaName, role, status, name, sortBy, sortDir, offset, size);
        long total = tenantStaffRepository.countStaff(schemaName, role, status, name);
        return PageResponseDTO.of(rows, total, p, size);
    }

    @Override
    public List<RoleCountDTO> countStaffByRole(String tenantCode, Integer status, String name) {
        String schemaName = TenantSchemaResolver.requireSchemaNameFromTenantCode(tenantCode);
        return tenantStaffRepository.countByRole(schemaName, status, name);
    }

    private int clampLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }
}
