package org.arghyam.jalsoochak.user.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.user.config.KeycloakProvider;
import org.arghyam.jalsoochak.user.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.user.dto.request.UpdateStaffRoleRequestDTO;
import org.arghyam.jalsoochak.user.dto.response.RoleCountDTO;
import org.arghyam.jalsoochak.user.dto.response.TenantStaffResponseDTO;
import org.arghyam.jalsoochak.user.exceptions.ResourceNotFoundException;
import org.arghyam.jalsoochak.user.repository.TenantStaffRepository;
import org.arghyam.jalsoochak.user.repository.TenantUserRecord;
import org.arghyam.jalsoochak.user.repository.UserCommonRepository;
import org.arghyam.jalsoochak.user.repository.UserTenantRepository;
import org.arghyam.jalsoochak.user.service.KeycloakAdminHelper;
import org.arghyam.jalsoochak.user.service.TenantStaffService;
import org.arghyam.jalsoochak.user.util.TenantSchemaResolver;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantStaffServiceImpl implements TenantStaffService {

    private final TenantStaffRepository tenantStaffRepository;
    private final UserTenantRepository userTenantRepository;
    private final UserCommonRepository userCommonRepository;
    private final KeycloakAdminHelper keycloakAdminHelper;
    private final KeycloakProvider keycloakProvider;

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

    @Override
    public TenantStaffResponseDTO updateStaffRole(Long id, UpdateStaffRoleRequestDTO request) {
        List<String> allowed = List.of("SECTION_OFFICER", "DISTRICT_OFFICER");
        if (!allowed.contains(request.newRole())) {
            throw new IllegalArgumentException("Role must be one of: " + allowed);
        }

        String schema = TenantSchemaResolver.requireSchemaNameFromTenantCode(request.tenantCode());
        TenantUserRecord user = userTenantRepository.findUserById(schema, id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        String currentRole = user.cName();
        if (currentRole.equals(request.newRole())) {
            throw new IllegalArgumentException("User already has role: " + request.newRole());
        }

        // Look up DB type ID before touching Keycloak — fail fast if role is unknown
        Long newUserTypeId = userCommonRepository.findUserTypeIdByName(request.newRole())
                .map(Integer::longValue)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown role: " + request.newRole()));

        String keycloakUuid = user.keycloakUuid();
        var usersResource = keycloakProvider.getAdminInstance().realm(keycloakProvider.getRealm()).users();

        try {
            keycloakAdminHelper.removeRoleFromUser(keycloakUuid, currentRole);
            keycloakAdminHelper.assignRoleToUser(keycloakUuid, request.newRole());

            UserRepresentation rep = usersResource.get(keycloakUuid).toRepresentation();
            Map<String, List<String>> attrs = new HashMap<>(
                    rep.getAttributes() != null ? rep.getAttributes() : Map.of());
            attrs.put("user_type", List.of(request.newRole()));
            rep.setAttributes(attrs);
            usersResource.get(keycloakUuid).update(rep);

            userTenantRepository.updateUserRole(schema, id, newUserTypeId);
        } catch (Exception e) {
            try {
                keycloakAdminHelper.assignRoleToUser(keycloakUuid, currentRole);
                keycloakAdminHelper.removeRoleFromUser(keycloakUuid, request.newRole());
                UserRepresentation rep = usersResource.get(keycloakUuid).toRepresentation();
                Map<String, List<String>> rollbackAttrs = new HashMap<>(
                        rep.getAttributes() != null ? rep.getAttributes() : Map.of());
                rollbackAttrs.put("user_type", List.of(currentRole));
                rep.setAttributes(rollbackAttrs);
                usersResource.get(keycloakUuid).update(rep);
            } catch (Exception compensateEx) {
                log.error("Keycloak compensation failed for user {}: {}", keycloakUuid, compensateEx.getMessage(), compensateEx);
            }
            throw e;
        }

        return tenantStaffRepository.findStaffById(schema, id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found after update: " + id));
    }

    private int clampLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }
}
