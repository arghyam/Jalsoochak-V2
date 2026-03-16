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
import org.arghyam.jalsoochak.user.enums.AdminUserStatus;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        log.info("inviteUser called");
        log.debug("inviteUser – role={}, tenantCode={}", request.getRole(), request.getTenantCode());
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
        if (existingUser.isPresent() && existingUser.get().status() != AdminUserStatus.PENDING) {
            throw new UserAlreadyExistsException("User with this email is already registered");
        }

        Integer adminLevelId = userCommonRepository.findUserTypeIdByName(request.getRole())
                .orElseThrow(() -> new BadRequestException("Unknown role: " + request.getRole()));

        Integer tenantId = "SUPER_USER".equals(request.getRole()) ? 0
                : userCommonRepository.findTenantIdByStateCode(request.getTenantCode())
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found for state code: " + request.getTenantCode()));

        // If a PENDING user already exists, reject if role/tenant metadata differs
        if (existingUser.isPresent()) {
            var pending = existingUser.get();
            if (!pending.adminLevel().equals(adminLevelId) || !pending.tenantId().equals(tenantId)) {
                throw new BadRequestException(
                        "A pending invitation exists for this email with different role or tenant. Revoke it before re-inviting.");
            }
            // Same role+tenant: fall through to re-send the invite token below
        } else {
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
        Instant expiresAt = Instant.now().plus(inviteProperties.expiryHours(), ChronoUnit.HOURS);
        userCommonRepository.upsertToken(request.getEmail(), hash, "INVITE", metadataJson, expiresAt,
                callerRow.id() != null ? callerRow.id().intValue() : null);

        String inviteUrl = UriComponentsBuilder
                .fromHttpUrl(frontendProperties.baseUrl())
                .path(frontendProperties.invitePath())
                .queryParam("token", raw)
                .build()
                .toUriString();
        mailService.sendMailAfterCommit(() -> mailService.sendInviteMail(request.getEmail(), inviteUrl));
    }

    @Override
    @Transactional
    public void reinviteUser(Long id, Authentication caller) {
        log.info("reinviteUser called");
        log.debug("reinviteUser – id={}", id);
        AdminUserRow target = userCommonRepository.findAdminUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (target.status() != AdminUserStatus.PENDING) {
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
        Instant expiresAt = Instant.now().plus(inviteProperties.expiryHours(), ChronoUnit.HOURS);
        userCommonRepository.upsertToken(target.email(), hash, "INVITE", metadataJson, expiresAt,
                callerRow.id() != null ? callerRow.id().intValue() : null);

        String inviteUrl = UriComponentsBuilder
                .fromHttpUrl(frontendProperties.baseUrl())
                .path(frontendProperties.invitePath())
                .queryParam("token", raw)
                .build()
                .toUriString();
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
        log.info("updateMe called");
        log.debug("updateMe – keycloakId={}", keycloakId);
        AdminUserRow user = userCommonRepository.findAdminUserByUuid(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.status() == AdminUserStatus.INACTIVE) {
            throw new BadRequestException("Cannot update a deactivated user");
        }

        var usersResource = keycloakProvider.getAdminInstance().realm(keycloakProvider.getRealm()).users();
        UserRepresentation rep = usersResource.get(keycloakId).toRepresentation();
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) rep.setFirstName(request.getFirstName());
        if (request.getLastName() != null && !request.getLastName().isBlank()) rep.setLastName(request.getLastName());

        if (request.getPhoneNumber() != null) {
            userCommonRepository.updateAdminUserProfile(user.id(), request.getPhoneNumber(), user.id());
        }

        String roleName = userCommonRepository.findUserTypeNameById(user.adminLevel()).orElse(null);
        if ("STATE_ADMIN".equals(roleName)
                && (request.getPhoneNumber() != null || request.getFirstName() != null || request.getLastName() != null)) {
            String tenantCode = userCommonRepository.findTenantStateCodeById(user.tenantId()).orElse(null);
            if (tenantCode != null) {
                String schema = "tenant_" + tenantCode.toLowerCase();
                String phoneToSet = request.getPhoneNumber() != null ? request.getPhoneNumber()
                        : userTenantRepository.findUserByEmail(schema, user.email())
                                .map(r -> r.phoneNumber()).orElse(null);
                String fn = rep.getFirstName() != null ? rep.getFirstName() : "";
                String ln = rep.getLastName() != null ? rep.getLastName() : "";
                userTenantRepository.updateUserProfile(schema, user.id(),
                        (fn + " " + ln).trim(), phoneToSet);
            }
        }

        usersResource.get(keycloakId).update(rep);

        return keycloakAdminHelper.buildAdminUserResponse(
                userCommonRepository.findAdminUserByUuid(keycloakId).orElse(user));
    }

    @Override
    public void changePassword(String keycloakId, ChangePasswordRequestDTO request) {
        log.info("changePassword called");
        log.debug("changePassword – keycloakId={}", keycloakId);
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
        long offset = (long) page * limit;
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
        Optional<String> callerRole = SecurityUtils.extractRole(caller);

        if (callerRole.map("STATE_ADMIN"::equals).orElse(false)) {
            String callerTenantCode = SecurityUtils.extractTenantCode(caller);
            if (callerTenantCode == null) {
                throw new ForbiddenAccessException("Unable to determine caller's tenant");
            }
            if (tenantCode != null && !tenantCode.equalsIgnoreCase(callerTenantCode)) {
                throw new ForbiddenAccessException("State admin can only list admins within their own state");
            }
            tenantId = userCommonRepository.findTenantIdByStateCode(callerTenantCode)
                    .orElseThrow(() -> new ForbiddenAccessException("Caller's tenant not found"));
        } else if (tenantCode != null && !tenantCode.isBlank()) {
            tenantId = userCommonRepository.findTenantIdByStateCode(tenantCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found for state code: " + tenantCode));
        }

        long offset = (long) page * limit;
        List<AdminUserRow> rows = userCommonRepository.listStateAdminsByTenant(tenantId, offset, limit);
        long total = userCommonRepository.countStateAdminsByTenant(tenantId);
        List<AdminUserResponseDTO> users = rows.stream()
                .map(keycloakAdminHelper::buildAdminUserResponse)
                .collect(Collectors.toList());
        return PageResponseDTO.of(users, total, page, limit);
    }

    @Override
    public AdminUserResponseDTO getUserById(Long id, Authentication caller) {
        AdminUserRow user = userCommonRepository.findAdminUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<String> callerRole = SecurityUtils.extractRole(caller);
        if (callerRole.map("STATE_ADMIN"::equals).orElse(false)) {
            String callerTenantCode = SecurityUtils.extractTenantCode(caller);
            String targetTenantCode = user.tenantId() != 0
                    ? userCommonRepository.findTenantStateCodeById(user.tenantId()).orElse(null) : null;
            if (callerTenantCode == null || !callerTenantCode.equalsIgnoreCase(targetTenantCode)) {
                throw new ForbiddenAccessException("Cannot view user from another state");
            }
        }

        return keycloakAdminHelper.buildAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDTO updateUserById(Long id, Authentication caller, UpdateProfileRequestDTO request) {
        log.info("updateUserById called");
        log.debug("updateUserById – id={}", id);
        AdminUserRow user = userCommonRepository.findAdminUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.status() == AdminUserStatus.PENDING) {
            throw new BadRequestException("Cannot update a user who has not completed registration");
        }
        if (user.status() == AdminUserStatus.INACTIVE) {
            throw new BadRequestException("Cannot update a deactivated user");
        }

        Optional<String> callerRole = SecurityUtils.extractRole(caller);
        if (callerRole.map("STATE_ADMIN"::equals).orElse(false)) {
            String callerTenantCode = SecurityUtils.extractTenantCode(caller);
            String targetTenantCode = user.tenantId() != 0
                    ? userCommonRepository.findTenantStateCodeById(user.tenantId()).orElse(null) : null;
            if (callerTenantCode == null || !callerTenantCode.equalsIgnoreCase(targetTenantCode)) {
                throw new ForbiddenAccessException("Cannot update user from another state");
            }
        }

        String callerUuid = SecurityUtils.getKeycloakId(caller);
        AdminUserRow callerRow = userCommonRepository.findAdminUserByUuid(callerUuid)
                .orElseThrow(() -> new UnauthorizedAccessException("Caller is not registered in the system"));

        var usersResource = keycloakProvider.getAdminInstance().realm(keycloakProvider.getRealm()).users();
        UserRepresentation rep = usersResource.get(user.uuid()).toRepresentation();
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) rep.setFirstName(request.getFirstName());
        if (request.getLastName() != null && !request.getLastName().isBlank()) rep.setLastName(request.getLastName());

        if (request.getPhoneNumber() != null) {
            userCommonRepository.updateAdminUserProfile(user.id(), request.getPhoneNumber(), callerRow.id());
        }

        String roleName = userCommonRepository.findUserTypeNameById(user.adminLevel()).orElse(null);
        if ("STATE_ADMIN".equals(roleName)
                && (request.getPhoneNumber() != null || request.getFirstName() != null || request.getLastName() != null)) {
            String tenantCode = userCommonRepository.findTenantStateCodeById(user.tenantId()).orElse(null);
            if (tenantCode != null) {
                String schema = "tenant_" + tenantCode.toLowerCase();
                String phoneToSet = request.getPhoneNumber() != null ? request.getPhoneNumber()
                        : userTenantRepository.findUserById(schema, user.id())
                                .map(r -> r.phoneNumber()).orElse(null);
                String fn = rep.getFirstName() != null ? rep.getFirstName() : "";
                String ln = rep.getLastName() != null ? rep.getLastName() : "";
                userTenantRepository.updateUserProfile(schema, user.id(),
                        (fn + " " + ln).trim(), phoneToSet);
            }
        }

        usersResource.get(user.uuid()).update(rep);

        return keycloakAdminHelper.buildAdminUserResponse(
                userCommonRepository.findAdminUserById(id).orElse(user));
    }

    @Override
    @Transactional
    public void deactivateUser(Long id, Authentication caller) {
        log.info("deactivateUser called");
        log.debug("deactivateUser – id={}", id);
        AdminUserRow target = userCommonRepository.findAdminUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String callerUuid = SecurityUtils.getKeycloakId(caller);
        if (callerUuid != null && callerUuid.equals(target.uuid())) {
            throw new ForbiddenAccessException("Cannot deactivate your own account");
        }

        String targetRole = userCommonRepository.findUserTypeNameById(target.adminLevel()).orElse("");
        Optional<String> callerRole = SecurityUtils.extractRole(caller);

        if (callerRole.map("STATE_ADMIN"::equals).orElse(false)) {
            String callerTenantCode = SecurityUtils.extractTenantCode(caller);
            String targetTenantCode = target.tenantId() != 0
                    ? userCommonRepository.findTenantStateCodeById(target.tenantId()).orElse(null) : null;
            if (callerTenantCode == null || !callerTenantCode.equalsIgnoreCase(targetTenantCode)) {
                throw new ForbiddenAccessException("Cannot deactivate user from another state");
            }
        }

        if (target.status() == AdminUserStatus.PENDING) {
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
        log.info("activateUser called");
        log.debug("activateUser – id={}", id);
        AdminUserRow target = userCommonRepository.findAdminUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (target.status() == AdminUserStatus.PENDING) {
            throw new BadRequestException("Cannot activate a user who has not completed registration");
        }

        String callerUuid = SecurityUtils.getKeycloakId(caller);
        AdminUserRow callerRow = userCommonRepository.findAdminUserByUuid(callerUuid)
                .orElseThrow(() -> new UnauthorizedAccessException("Caller is not registered in the system"));

        Optional<String> callerRole = SecurityUtils.extractRole(caller);
        if (callerRole.map("STATE_ADMIN"::equals).orElse(false)) {
            String callerTenantCode = SecurityUtils.extractTenantCode(caller);
            String targetTenantCode = target.tenantId() != 0
                    ? userCommonRepository.findTenantStateCodeById(target.tenantId()).orElse(null) : null;
            if (callerTenantCode == null || !callerTenantCode.equalsIgnoreCase(targetTenantCode)) {
                throw new UnauthorizedAccessException("Cannot activate user from another state");
            }
        }

        userCommonRepository.activateAdminUser(id, callerRow.id());

        var usersResource = keycloakProvider.getAdminInstance().realm(keycloakProvider.getRealm()).users();
        UserRepresentation rep = usersResource.get(target.uuid()).toRepresentation();
        rep.setEnabled(true);
        usersResource.get(target.uuid()).update(rep);
    }
}
