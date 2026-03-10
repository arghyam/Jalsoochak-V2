package org.arghyam.jalsoochak.user.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.user.clients.KeycloakClient;
import org.arghyam.jalsoochak.user.config.KeycloakProvider;
import org.arghyam.jalsoochak.user.config.properties.FrontendProperties;
import org.arghyam.jalsoochak.user.config.properties.InviteProperties;
import org.arghyam.jalsoochak.user.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.user.dto.request.ChangePasswordRequestDTO;
import org.arghyam.jalsoochak.user.dto.request.InviteRequestDTO;
import org.arghyam.jalsoochak.user.dto.request.UpdateProfileRequestDTO;
import org.arghyam.jalsoochak.user.dto.response.AdminUserResponseDTO;
import org.arghyam.jalsoochak.user.exceptions.BadRequestException;
import org.arghyam.jalsoochak.user.exceptions.ForbiddenAccessException;
import org.arghyam.jalsoochak.user.exceptions.InsufficientActiveUsersException;
import org.arghyam.jalsoochak.user.exceptions.InvalidCredentialsException;
import org.arghyam.jalsoochak.user.exceptions.ResourceNotFoundException;
import org.arghyam.jalsoochak.user.exceptions.UnauthorizedAccessException;
import org.arghyam.jalsoochak.user.exceptions.UserAlreadyExistsException;
import org.arghyam.jalsoochak.user.repository.UserCommonRepository;
import org.arghyam.jalsoochak.user.repository.UserTenantRepository;
import org.arghyam.jalsoochak.user.repository.records.AdminUserRow;
import org.arghyam.jalsoochak.user.service.KeycloakAdminHelper;
import org.arghyam.jalsoochak.user.service.MailService;
import org.arghyam.jalsoochak.user.service.TokenService;
import org.arghyam.jalsoochak.user.service.UserManagementService;
import org.arghyam.jalsoochak.user.util.SecurityUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final KeycloakProvider keycloakProvider;
    private final KeycloakClient keycloakClient;
    private final UserCommonRepository userCommonRepository;
    private final UserTenantRepository userTenantRepository;
    private final MailService mailService;
    private final KeycloakAdminHelper keycloakAdminHelper;
    private final InviteProperties inviteProperties;
    private final FrontendProperties frontendProperties;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void inviteUser(InviteRequestDTO request, Authentication caller) {
        String callerUuid = SecurityUtils.getKeycloakId(caller);
        AdminUserRow callerRow = userCommonRepository.findAdminUserByUuid(callerUuid)
                .orElseThrow(() -> new UnauthorizedAccessException("Caller is not registered in the system"));

        String callerRole = userCommonRepository.findUserTypeNameById(callerRow.adminLevel()).orElse("");

        if ("STATE_ADMIN".equals(callerRole)) {
            if (!"STATE_ADMIN".equals(request.getRole())) {
                throw new ForbiddenAccessException("State admin can only invite state admins");
            }
            String callerTenantCode = SecurityUtils.extractTenantCode(caller);
            if (callerTenantCode == null || !callerTenantCode.equalsIgnoreCase(request.getTenantCode())) {
                throw new ForbiddenAccessException("State admin can only invite within their own state");
            }
        }

        if ("STATE_ADMIN".equals(request.getRole())) {
            if (request.getTenantCode() == null || request.getTenantCode().isBlank()) {
                throw new BadRequestException("tenantCode is required for STATE_ADMIN role");
            }
            if (!userCommonRepository.existsTenantByStateCode(request.getTenantCode())) {
                throw new ResourceNotFoundException("Tenant not found for state code: " + request.getTenantCode());
            }
        }

        var existingUser = userCommonRepository.findAdminUserByEmail(request.getEmail());
        if (existingUser.isPresent() && existingUser.get().status() != 2) {
            throw new UserAlreadyExistsException("User with this email is already registered");
        }

        Integer adminLevelId = userCommonRepository.findUserTypeIdByName(request.getRole())
                .orElseThrow(() -> new BadRequestException("Unknown role: " + request.getRole()));

        Integer tenantId = "SUPER_USER".equals(request.getRole()) ? 0
                : userCommonRepository.findTenantIdByStateCode(request.getTenantCode())
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found for state code: " + request.getTenantCode()));

        // Create a PENDING user record if this is a fresh invite (not a reinvite of an existing PENDING user)
        if (existingUser.isEmpty()) {
            userCommonRepository.createAdminUserPending(request.getEmail(), tenantId, adminLevelId,
                    callerRow.id() != null ? callerRow.id().intValue() : null);
        }

        String tenantName = "STATE_ADMIN".equals(request.getRole())
                ? userCommonRepository.findTenantTitleByStateCode(request.getTenantCode()).orElse(null)
                : null;

        Map<String, Object> metaMap = new HashMap<>();
        metaMap.put("role", request.getRole());
        if (request.getTenantCode() != null) metaMap.put("tenantCode", request.getTenantCode());
        if (tenantName != null) metaMap.put("tenantName", tenantName);

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metaMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize invite metadata", e);
        }

        String raw = tokenService.generateRawToken();
        String hash = tokenService.hash(raw);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(inviteProperties.expiryHours());
        userCommonRepository.upsertToken(request.getEmail(), hash, "INVITE", metadataJson, expiresAt,
                callerRow.id() != null ? callerRow.id().intValue() : null);

        String inviteUrl = frontendProperties.baseUrl() + frontendProperties.invitePath() + "?token=" + raw;
        mailService.sendMailAfterCommit(() -> mailService.sendInviteMail(request.getEmail(), inviteUrl));
    }

    @Override
    @Transactional
    public void reinviteUser(Long id, Authentication caller) {
        AdminUserRow target = userCommonRepository.findAdminUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (target.status() != 2) {
            throw new BadRequestException("User has already activated their account");
        }

        String callerUuid = SecurityUtils.getKeycloakId(caller);
        AdminUserRow callerRow = userCommonRepository.findAdminUserByUuid(callerUuid)
                .orElseThrow(() -> new UnauthorizedAccessException("Caller is not registered in the system"));

        String callerRole = userCommonRepository.findUserTypeNameById(callerRow.adminLevel()).orElse("");
        String targetRole = userCommonRepository.findUserTypeNameById(target.adminLevel()).orElse("");

        if ("STATE_ADMIN".equals(callerRole)) {
            if (!"STATE_ADMIN".equals(targetRole)) {
                throw new ForbiddenAccessException("State admin can only reinvite state admins");
            }
            String callerTenantCode = SecurityUtils.extractTenantCode(caller);
            String targetTenantCode = target.tenantId() != 0
                    ? userCommonRepository.findTenantStateCodeById(target.tenantId()).orElse(null) : null;
            if (callerTenantCode == null || !callerTenantCode.equalsIgnoreCase(targetTenantCode)) {
                throw new ForbiddenAccessException("State admin can only reinvite within their own state");
            }
        }

        String tenantCode = target.tenantId() != 0
                ? userCommonRepository.findTenantStateCodeById(target.tenantId()).orElse(null) : null;
        String tenantName = tenantCode != null
                ? userCommonRepository.findTenantTitleByStateCode(tenantCode).orElse(null) : null;

        Map<String, Object> metaMap = new HashMap<>();
        metaMap.put("role", targetRole);
        if (tenantCode != null) metaMap.put("tenantCode", tenantCode);
        if (tenantName != null) metaMap.put("tenantName", tenantName);

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metaMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize invite metadata", e);
        }

        String raw = tokenService.generateRawToken();
        String hash = tokenService.hash(raw);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(inviteProperties.expiryHours());
        userCommonRepository.upsertToken(target.email(), hash, "INVITE", metadataJson, expiresAt,
                callerRow.id() != null ? callerRow.id().intValue() : null);

        String inviteUrl = frontendProperties.baseUrl() + frontendProperties.invitePath() + "?token=" + raw;
        mailService.sendMailAfterCommit(() -> mailService.sendInviteMail(target.email(), inviteUrl));
    }

    @Override
    public AdminUserResponseDTO getMe(String keycloakId) {
        AdminUserRow user = userCommonRepository.findAdminUserByUuid(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return keycloakAdminHelper.buildAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDTO updateMe(String keycloakId, UpdateProfileRequestDTO request) {
        AdminUserRow user = userCommonRepository.findAdminUserByUuid(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var usersResource = keycloakProvider.getAdminInstance().realm(keycloakProvider.getRealm()).users();
        UserRepresentation rep = usersResource.get(keycloakId).toRepresentation();
        if (request.getFirstName() != null) rep.setFirstName(request.getFirstName());
        if (request.getLastName() != null) rep.setLastName(request.getLastName());
        usersResource.get(keycloakId).update(rep);

        if (request.getPhoneNumber() != null) {
            userCommonRepository.updateAdminUserProfile(user.id(), request.getPhoneNumber(), user.id());
        }

        String roleName = userCommonRepository.findUserTypeNameById(user.adminLevel()).orElse(null);
        if ("STATE_ADMIN".equals(roleName) && request.getPhoneNumber() != null) {
            String tenantCode = userCommonRepository.findTenantStateCodeById(user.tenantId()).orElse(null);
            if (tenantCode != null) {
                userTenantRepository.updateUserProfile(
                        "tenant_" + tenantCode.toLowerCase(),
                        user.id(),
                        rep.getFirstName() + " " + rep.getLastName(),
                        request.getPhoneNumber());
            }
        }

        return keycloakAdminHelper.buildAdminUserResponse(
                userCommonRepository.findAdminUserByUuid(keycloakId).orElse(user));
    }

    @Override
    public void changePassword(String keycloakId, ChangePasswordRequestDTO request) {
        AdminUserRow user = userCommonRepository.findAdminUserByUuid(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            keycloakClient.obtainToken(user.email(), request.getCurrentPassword());
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new InvalidCredentialsException("Current password is incorrect");
            }
            throw e;
        }

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(request.getNewPassword());
        cred.setTemporary(false);

        var usersResource = keycloakProvider.getAdminInstance().realm(keycloakProvider.getRealm()).users();
        usersResource.get(keycloakId).resetPassword(cred);
        usersResource.get(keycloakId).logout();
    }

    @Override
    public PageResponseDTO<AdminUserResponseDTO> listSuperUsers(int page, int limit) {
        int offset = page * limit;
        List<AdminUserRow> rows = userCommonRepository.listSuperUsers(offset, limit);
        long total = userCommonRepository.countSuperUsers();
        List<AdminUserResponseDTO> users = rows.stream()
                .map(keycloakAdminHelper::buildAdminUserResponse)
                .collect(Collectors.toList());
        return PageResponseDTO.of(users, total, page, limit);
    }

    @Override
    public PageResponseDTO<AdminUserResponseDTO> listStateAdmins(String tenantCode, Authentication caller, int page, int limit) {
        Integer tenantId = null;
        String callerRole = SecurityUtils.extractRole(caller);

        if ("STATE_ADMIN".equals(callerRole)) {
            String callerTenantCode = SecurityUtils.extractTenantCode(caller);
            if (callerTenantCode == null) {
                throw new ForbiddenAccessException("Unable to determine caller's tenant");
            }
            tenantId = userCommonRepository.findTenantIdByStateCode(callerTenantCode)
                    .orElseThrow(() -> new ForbiddenAccessException("Caller's tenant not found"));
        } else if (tenantCode != null && !tenantCode.isBlank()) {
            tenantId = userCommonRepository.findTenantIdByStateCode(tenantCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found for state code: " + tenantCode));
        }

        int offset = page * limit;
        List<AdminUserRow> rows = userCommonRepository.listStateAdminsByTenant(tenantId, offset, limit);
        long total = userCommonRepository.countStateAdminsByTenant(tenantId);
        List<AdminUserResponseDTO> users = rows.stream()
                .map(keycloakAdminHelper::buildAdminUserResponse)
                .collect(Collectors.toList());
        return PageResponseDTO.of(users, total, page, limit);
    }

    @Override
    public AdminUserResponseDTO getUserById(Long id) {
        AdminUserRow user = userCommonRepository.findAdminUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return keycloakAdminHelper.buildAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDTO updateUserById(Long id, UpdateProfileRequestDTO request) {
        AdminUserRow user = userCommonRepository.findAdminUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.status() == 2) {
            throw new BadRequestException("Cannot update a user who has not completed registration");
        }

        var usersResource = keycloakProvider.getAdminInstance().realm(keycloakProvider.getRealm()).users();
        UserRepresentation rep = usersResource.get(user.uuid()).toRepresentation();
        if (request.getFirstName() != null) rep.setFirstName(request.getFirstName());
        if (request.getLastName() != null) rep.setLastName(request.getLastName());
        usersResource.get(user.uuid()).update(rep);

        if (request.getPhoneNumber() != null) {
            userCommonRepository.updateAdminUserProfile(user.id(), request.getPhoneNumber(), user.id());
        }

        String roleName = userCommonRepository.findUserTypeNameById(user.adminLevel()).orElse(null);
        if ("STATE_ADMIN".equals(roleName) && request.getPhoneNumber() != null) {
            String tenantCode = userCommonRepository.findTenantStateCodeById(user.tenantId()).orElse(null);
            if (tenantCode != null) {
                userTenantRepository.updateUserProfile(
                        "tenant_" + tenantCode.toLowerCase(),
                        user.id(),
                        rep.getFirstName() + " " + rep.getLastName(),
                        request.getPhoneNumber());
            }
        }

        return keycloakAdminHelper.buildAdminUserResponse(
                userCommonRepository.findAdminUserById(id).orElse(user));
    }

    @Override
    @Transactional
    public void deactivateUser(Long id, Authentication caller) {
        AdminUserRow target = userCommonRepository.findAdminUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String targetRole = userCommonRepository.findUserTypeNameById(target.adminLevel()).orElse("");
        String callerRole = SecurityUtils.extractRole(caller);

        if ("STATE_ADMIN".equals(callerRole)) {
            String callerTenantCode = SecurityUtils.extractTenantCode(caller);
            String targetTenantCode = target.tenantId() != 0
                    ? userCommonRepository.findTenantStateCodeById(target.tenantId()).orElse(null) : null;
            if (callerTenantCode == null || !callerTenantCode.equalsIgnoreCase(targetTenantCode)) {
                throw new ForbiddenAccessException("Cannot deactivate user from another state");
            }
        }

        if (target.status() == 2) {
            throw new BadRequestException("Cannot deactivate a user who has not completed registration");
        }

        if ("SUPER_USER".equals(targetRole)) {
            if (userCommonRepository.countActiveSuperUsers() <= 1) {
                throw new InsufficientActiveUsersException("At least one active super user must remain");
            }
        } else if ("STATE_ADMIN".equals(targetRole)) {
            if (userCommonRepository.countActiveStateAdminsForTenant(target.tenantId()) <= 1) {
                throw new InsufficientActiveUsersException("At least one active state admin must remain for this state");
            }
        }

        String callerUuid = SecurityUtils.getKeycloakId(caller);
        AdminUserRow callerRow = userCommonRepository.findAdminUserByUuid(callerUuid)
                .orElseThrow(() -> new UnauthorizedAccessException("Caller is not registered in the system"));

        userCommonRepository.deactivateAdminUser(id, callerRow.id());

        var usersResource = keycloakProvider.getAdminInstance().realm(keycloakProvider.getRealm()).users();
        UserRepresentation rep = usersResource.get(target.uuid()).toRepresentation();
        rep.setEnabled(false);
        usersResource.get(target.uuid()).update(rep);
    }

    @Override
    @Transactional
    public void activateUser(Long id, Authentication caller) {
        AdminUserRow target = userCommonRepository.findAdminUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (target.status() == 2) {
            throw new BadRequestException("Cannot activate a user who has not completed registration");
        }

        String callerUuid = SecurityUtils.getKeycloakId(caller);
        AdminUserRow callerRow = userCommonRepository.findAdminUserByUuid(callerUuid)
                .orElseThrow(() -> new UnauthorizedAccessException("Caller is not registered in the system"));

        userCommonRepository.activateAdminUser(id, callerRow.id());

        var usersResource = keycloakProvider.getAdminInstance().realm(keycloakProvider.getRealm()).users();
        UserRepresentation rep = usersResource.get(target.uuid()).toRepresentation();
        rep.setEnabled(true);
        usersResource.get(target.uuid()).update(rep);
    }
}
