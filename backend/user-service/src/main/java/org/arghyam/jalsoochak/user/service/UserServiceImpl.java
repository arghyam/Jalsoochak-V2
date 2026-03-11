package org.arghyam.jalsoochak.user.service;

import org.arghyam.jalsoochak.user.clients.KeycloakClient;
import org.arghyam.jalsoochak.user.config.KeycloakProvider;
import org.arghyam.jalsoochak.user.config.TenantContext;
import org.arghyam.jalsoochak.user.dto.request.InviteRequest;
import org.arghyam.jalsoochak.user.dto.request.LoginRequest;
import org.arghyam.jalsoochak.user.dto.request.RegisterRequest;
import org.arghyam.jalsoochak.user.dto.response.TokenResponse;
import org.arghyam.jalsoochak.user.repository.InviteTokenRow;
import org.arghyam.jalsoochak.user.repository.TenantUserRecord;
import org.arghyam.jalsoochak.user.repository.UserCommonRepository;
import org.arghyam.jalsoochak.user.repository.UserTenantRepository;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Value("${keycloak.realm}")
    public String realm;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    private final KeycloakProvider keycloakProvider;
    private final MailService mailService;
    private final UserTenantRepository userTenantRepository;
    private final KeycloakClient keycloakClient;
    private final UserCommonRepository userCommonRepository;
    private static final String SUPER_USER_ROLE = "super_user";

    public UserServiceImpl(KeycloakProvider keycloakProvider,
                           KeycloakClient keycloakClient, MailService mailService,
                           UserTenantRepository userTenantRepository, UserCommonRepository userCommonRepository) {
        this.keycloakProvider = keycloakProvider;
        this.keycloakClient = keycloakClient;
        this.mailService = mailService;
        this.userTenantRepository = userTenantRepository;
        this.userCommonRepository = userCommonRepository;
    }


    @Transactional
    public void inviteUser(InviteRequest inviteRequest) {

        if (inviteRequest.getSenderId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sender ID is required"
            );
        }

        String schemaName = TenantContext.getSchema();
        log.info("schemaName: {}", schemaName);
        if (schemaName == null || schemaName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tenant could not be resolved. Ensure X-Tenant-Code header is set."
            );
        }

        TenantUserRecord sender = userTenantRepository
                .findUserById(schemaName, inviteRequest.getSenderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sender does not exist"
                ));

        if (sender.cName() == null ||
                !sender.cName().equalsIgnoreCase(SUPER_USER_ROLE)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only super user can send invitations"
            );
        }

        String inviteeEmail = inviteRequest.getEmail().trim().toLowerCase();
        if (userTenantRepository.existsEmail(schemaName, inviteeEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User with this email already exists"
            );
        }

        if (userCommonRepository.existsActiveInviteByEmail(inviteeEmail, sender.tenantId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An active invitation already exists for this email"
            );
        }

        String token = UUID.randomUUID().toString();
        userCommonRepository.createInviteToken(
                inviteeEmail,
                token,
                LocalDateTime.now().plusHours(24),
                sender.tenantId(),
                inviteRequest.getSenderId()
        );

        String inviteLink = frontendBaseUrl + "?token=" + token;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    mailService.sendInviteMail(inviteeEmail, inviteLink);
                } catch (Exception e) {
                    log.error("Failed to send invite email to {}", inviteeEmail, e);
                }
            }
        });
    }


    @Transactional
    public void completeProfile(RegisterRequest registerRequest) {

        InviteTokenRow inviteToken = userCommonRepository.findInviteTokenByToken(registerRequest.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invite token"));

        if (!inviteToken.email().equalsIgnoreCase(registerRequest.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token does not belong to this user");
        }

        if (inviteToken.used()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite token has already been used");
        }

        if (inviteToken.expiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite token has expired");
        }

        String tenantCode = userCommonRepository.findTenantStateCodeById(inviteToken.tenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenant for invite token"))
                .trim()
                .toLowerCase();

        if (registerRequest.getTenantId() != null
                && !registerRequest.getTenantId().isBlank()
                && !tenantCode.equals(registerRequest.getTenantId().trim().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant does not match invite token");
        }

        String schemaName = "tenant_" + tenantCode;
        Integer tenantId = inviteToken.tenantId();

        Integer userTypeId = userCommonRepository.findUserTypeId(registerRequest.getPersonType())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid person type"));

        if (userTenantRepository.existsPhoneNumber(schemaName, registerRequest.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already exists");
        }

        String inviteeEmail = inviteToken.email().trim().toLowerCase();
        if (userTenantRepository.existsEmail(schemaName, inviteeEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UsersResource usersResource = keycloakProvider.getAdminInstance()
                .realm(realm)
                .users();

        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setUsername(registerRequest.getPhoneNumber());
        keycloakUser.setEmail(inviteeEmail);
        keycloakUser.setFirstName(registerRequest.getFirstName());
        keycloakUser.setLastName(registerRequest.getLastName());
        keycloakUser.setEnabled(true);
        keycloakUser.setEmailVerified(true);

        String keycloakUserId;
        try (Response response = usersResource.create(keycloakUser)) {

            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Failed to create user in Keycloak"
                );
            }

            keycloakUserId = response.getLocation().getPath().replaceAll(".*/", "");
        }

        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(registerRequest.getPassword());
            credential.setTemporary(false);
            usersResource.get(keycloakUserId).resetPassword(credential);

            String title = (registerRequest.getFirstName() + " " + registerRequest.getLastName()).trim();
            userTenantRepository.createUser(
                    schemaName,
                    tenantId,
                    title,
                    inviteeEmail,
                    userTypeId,
                    registerRequest.getPhoneNumber(),
                    inviteToken.senderId()
            );

            assignRoleToUser(keycloakUserId, "STATE_ADMIN");

            userCommonRepository.markInviteTokenUsed(inviteToken.id());

            log.info("Profile completed successfully for user: {}", inviteeEmail);

        } catch (Exception e) {
            log.error("Failed to complete profile, deleting Keycloak user to avoid orphaned account", e);
            try {
                usersResource.delete(keycloakUserId);
            } catch (Exception kcEx) {
                log.error("Failed to delete Keycloak user {} after DB failure", keycloakUserId, kcEx);
            }
            throw e;
        }
    }

    public TokenResponse login(LoginRequest loginRequest, String tenantCode) {
        if (!userCommonRepository.existsTenantByStateCode(tenantCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenant code");
        }

        String schemaName = "tenant_" + tenantCode.toLowerCase().trim();

        TenantUserRecord user = userTenantRepository
                .findUserByPhone(schemaName, loginRequest.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "User not found in tenant"));

        log.debug("User '{}' logged in with tenant '{}'", user.phoneNumber(), tenantCode);
        Map<String, Object> tokenMap = keycloakClient.obtainToken(
                loginRequest.getUsername(), loginRequest.getPassword()
        );


        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken((String) tokenMap.get("access_token"));
        tokenResponse.setRefreshToken((String) tokenMap.get("refresh_token"));
        tokenResponse.setExpiresIn(tokenMap.get("expires_in") instanceof Number ? ((Number) tokenMap.get("expires_in")).intValue() : 0);
        tokenResponse.setRefreshExpiresIn(tokenMap.get("refresh_expires_in") instanceof Number ? ((Number) tokenMap.get("refresh_expires_in")).intValue() : 0);
        tokenResponse.setTokenType((String) tokenMap.get("token_type"));
        tokenResponse.setIdToken((String) tokenMap.get("id_token"));
        tokenResponse.setSessionState((String) tokenMap.get("session_state"));
        tokenResponse.setScope((String) tokenMap.get("scope"));

        tokenResponse.setPersonId(user.id());
        tokenResponse.setTenantId(tenantCode);
        tokenResponse.setRole(user.cName());

        return tokenResponse;
    }


    public TokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token must be provided");
        }

        Map<String, Object> tokenMap = keycloakClient.refreshToken(refreshToken);

        String accessToken = (String) tokenMap.get("access_token");
        Map<String, Object> userInfo = keycloakClient.getUserInfo(accessToken);

        String username = (String) userInfo.get("preferred_username");
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to extract username from token");
        }

        String tenantCode = getTenantCodeFromUserInfo(userInfo);
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to extract tenant code from token");
        }

        if (!userCommonRepository.existsTenantByStateCode(tenantCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenant code in token");
        }

        String schemaName = "tenant_" + tenantCode.toLowerCase().trim();
        TenantUserRecord user = userTenantRepository.findUserByPhone(schemaName, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found in tenant"));

        log.debug("User '{}' refreshed token with tenant '{}'", user.phoneNumber(), tenantCode);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(accessToken);
        tokenResponse.setRefreshToken((String) tokenMap.get("refresh_token"));

        tokenResponse.setExpiresIn(tokenMap.get("expires_in") instanceof Number ? ((Number) tokenMap.get("expires_in")).intValue() : 0);
        tokenResponse.setRefreshExpiresIn(tokenMap.get("refresh_expires_in") instanceof Number ? ((Number) tokenMap.get("refresh_expires_in")).intValue() : 0);
        tokenResponse.setTokenType((String) tokenMap.get("token_type"));
        tokenResponse.setIdToken((String) tokenMap.get("id_token"));
        tokenResponse.setSessionState((String) tokenMap.get("session_state"));
        tokenResponse.setScope((String) tokenMap.get("scope"));
        tokenResponse.setPersonId(user.id());
        tokenResponse.setTenantId(tenantCode);
        tokenResponse.setRole(user.cName());

        return tokenResponse;
    }

    private String getTenantCodeFromUserInfo(Map<String, Object> userInfo) {
        Object tenantStateCode = userInfo.get("tenant_state_code");
        if (tenantStateCode instanceof String value && !value.isBlank()) {
            return value;
        }

        Object tenantCode = userInfo.get("tenant_code");
        if (tenantCode instanceof String value && !value.isBlank()) {
            return value;
        }

        return null;
    }

    public boolean logout(String refreshToken) {
        return keycloakClient.logout(refreshToken);
    }

    private void assignRoleToUser(String userId, String roleName){
        RealmResource realmResource =
                keycloakProvider.getAdminInstance()
                        .realm(realm);

        RoleRepresentation role =
                realmResource.roles().get(roleName).toRepresentation();

        realmResource
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(List.of(role));

        log.debug("Assigned role '{}' to Keycloak user with id '{}'", roleName, userId);
    }
}
