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

//    @Transactional
//    public Map<String, Object> bulkInviteUsers(MultipartFile file, String tenantCode) {
//        if (file.isEmpty()) {
//            throw new BadRequestException("File is empty");
//        }
//
//        String filename = file.getOriginalFilename();
//        if (filename == null) {
//            throw new BadRequestException("File name is missing");
//        }
//
//        List<Map<String, String>> errors = new ArrayList<>();
//        List<PersonMaster> personsToSave = new ArrayList<>();
//        List<PersonSchemeMapping> mappingsToSave = new ArrayList<>();
//        Set<String> phoneNumbers = new HashSet<>();
//
//        String[] EXPECTED_HEADERS = {
//                "first_name",
//                "last_name",
//                "full_name",
//                "phone_number",
//                "alternate_number",
//                "person_type_id",
//                "state_scheme_id",
//                "center_scheme_id"
//        };
//
//        String schemaName = TenantContext.getSchema();
//        if (schemaName == null || schemaName.isBlank()) {
//            schemaName = "tenant_" + tenantCode.toLowerCase().trim();
//        }
//
//        try (InputStream is = file.getInputStream()) {
//
//            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
//                try (Workbook workbook = filename.endsWith(".xlsx") ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {
//                    Sheet sheet = workbook.getSheetAt(0);
//
//                    Row headerRow = sheet.getRow(0);
//                    if (headerRow == null) {
//                        throw new org.arghyam.jalsoochak.user.exceptions.BadRequestException("Header row is missing");
//                    }
//
//                    for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
//                        String cellValue = getCellValue(headerRow.getCell(i)).toLowerCase().trim();
//                        if (!EXPECTED_HEADERS[i].equals(cellValue)) {
//                            throw new org.arghyam.jalsoochak.user.exceptions.BadRequestException("Invalid header at column " + (i + 1)
//                                    + ": expected '" + EXPECTED_HEADERS[i] + "', found '" + cellValue + "'");
//                        }
//                    }
//
//                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                        Row row = sheet.getRow(i);
//                        if (row == null) continue;
//
//                        String[] cells = new String[8];
//                        for (int c = 0; c < 8; c++) {
//                            cells[c] = getCellValue(row.getCell(c));
//                        }
//
//                        processRow(cells, i + 1, tenantCode, schemaName, phoneNumbers, personsToSave, mappingsToSave, errors);
//                    }
//                }
//            } else if (filename.endsWith(".csv")) {
//                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//                     CSVReader csvReader = new CSVReader(reader)) {
//
//                    String[] headerRow = csvReader.readNext();
//                    if (headerRow == null) {
//                        throw new org.arghyam.jalsoochak.user.exceptions.BadRequestException("CSV file is empty or missing header row");
//                    }
//
//                    for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
//                        String cellValue = i < headerRow.length ? headerRow[i].toLowerCase().trim() : "";
//                        if (!EXPECTED_HEADERS[i].equals(cellValue)) {
//                            throw new org.arghyam.jalsoochak.user.exceptions.BadRequestException("Invalid CSV header at column " + (i + 1)
//                                    + ": expected '" + EXPECTED_HEADERS[i] + "', found '" + cellValue + "'");
//                        }
//                    }
//
//                    String[] cells;
//                    int rowNum = 1;
//                    while ((cells = csvReader.readNext()) != null) {
//                        rowNum++;
//
//                        String[] paddedCells = new String[8];
//                        for (int i = 0; i < 8; i++) {
//                            if (i < cells.length) {
//                                paddedCells[i] = cells[i] != null ? cells[i].trim() : "";
//                            } else {
//                                paddedCells[i] = "";
//                            }
//                        }
//
//                        processRow(paddedCells, rowNum, tenantCode, schemaName, phoneNumbers, personsToSave, mappingsToSave, errors);
//                    }
//                } catch (CsvValidationException e) {
//                    throw new BadRequestException("Invalid CSV format: " + e.getMessage());
//                }
//            } else {
//                throw new BadRequestException("Unsupported file type: " + filename);
//            }
//
//            if (!errors.isEmpty()) {
//                throw new org.arghyam.jalsoochak.user.exceptions.BadRequestException("Validation failed", errors);
//            }
//
//            personMasterRepository.saveAll(personsToSave);
//            personSchemeMappingRepository.saveAll(mappingsToSave);
//
//            return Map.of(
//                    "message", "Onboarding Successful",
//                    "count", personsToSave.size()
//            );
//
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to read file", e);
//        }
//    }
//
//
//    private void processRow(String[] cells, int rowNum, String tenantCode, String schemaName,
//                            Set<String> phoneNumbers,
//                            List<PersonMaster> personsToSave,
//                            List<PersonSchemeMapping> mappingsToSave,
//                            List<Map<String, String>> errors) {
//
//        String firstName = cells[0];
//        String lastName = cells[1];
//        String fullName = cells[2];
//        String phoneNumber = cells[3];
//        String alternateNumber = cells[4];
//        String personTypeTitle = cells[5];
//        String stateSchemeIdStr = cells[6];
//        String centerSchemeIdStr = cells[7];
//
//        Map<String, String> rowErrors = new HashMap<>();
//
//        if (firstName == null || firstName.isBlank()) {
//            rowErrors.put("first_name", "First name is required");
//        } else if (!firstName.matches(NAME_REGEX)) {
//            rowErrors.put("first_name", "First name must contain only letters");
//        }
//
//        if (lastName == null || lastName.isBlank()) {
//            rowErrors.put("last_name", "Last name is required");
//        } else if (!lastName.matches(NAME_REGEX)) {
//            rowErrors.put("last_name", "Last name must contain only letters");
//        }
//
//        if (fullName == null || fullName.isBlank()) {
//            rowErrors.put("full_name", "Full name is required");
//        } else if (!fullName.matches(FULL_NAME_REGEX)) {
//            rowErrors.put("full_name", "Full name must contain only letters and spaces");
//        }
//
//        if (phoneNumber == null || phoneNumber.isBlank()) {
//            rowErrors.put("phone_number", "Phone number is required");
//        } else if (!phoneNumber.matches(PHONE_REGEX)) {
//            rowErrors.put("phone_number", "Phone number must be exactly 10 digits");
//        } else {
//            if (!phoneNumbers.add(phoneNumber)) {
//                rowErrors.put("phone_number", "Duplicate phone number in file");
//            }
//            if (userTenantRepository.existsPhoneNumber(schemaName, phoneNumber)
//                    || personMasterRepository.existsByPhoneNumberAndTenantId(phoneNumber, tenantCode)) {
//                rowErrors.put("phone_number", "Phone number already exists in system");
//            }
//        }
//
//
//        if (personTypeTitle == null || personTypeTitle.isBlank())
//            rowErrors.put("person_type", "person_type is required");
//        else if (!ALLOWED_PERSON_TYPE.equalsIgnoreCase(personTypeTitle.trim()))
//            rowErrors.put("person_type", "person_type must be 'Pump Operator'");
//
//        if (stateSchemeIdStr == null || stateSchemeIdStr.isBlank())
//            rowErrors.put("state_scheme_id", "State scheme id is required");
//        if (centerSchemeIdStr == null || centerSchemeIdStr.isBlank())
//            rowErrors.put("center_scheme_id", "Center scheme id is required");
//
//        if (!rowErrors.isEmpty()) {
//            rowErrors.put("row", String.valueOf(rowNum));
//            errors.add(rowErrors);
//            return;
//        }
//
//        PersonTypeMaster personType = personTypeMasterRepository.findByTitle(ALLOWED_PERSON_TYPE).orElse(null);
//        if (personType == null) {
//            rowErrors.put("person_type", "'Pump Operator' not configured in system");
//            rowErrors.put("row", String.valueOf(rowNum));
//            errors.add(rowErrors);
//            return;
//        }
//
//        PersonMaster person = PersonMaster.builder()
//                .firstName(firstName)
//                .lastName(lastName)
//                .fullName(fullName)
//                .phoneNumber(phoneNumber)
//                .alternateNumber(alternateNumber)
//                .tenantId(tenantCode)
//                .personType(personType)
//                .build();
//        personsToSave.add(person);
//
//        if (stateSchemeIdStr != null && !stateSchemeIdStr.isBlank()) {
//            try {
//                Long stateSchemeId = Long.parseLong(stateSchemeIdStr);
//                SchemeMaster stateScheme = schemeMasterRepository.findById(stateSchemeId)
//                        .orElseThrow(() -> new BadRequestException("Invalid state scheme at row " + rowNum));
//
//                mappingsToSave.add(
//                        PersonSchemeMapping.builder()
//                                .person(person)
//                                .scheme(stateScheme)
//                                .build()
//                );
//            } catch (NumberFormatException e) {
//                rowErrors.put("state_scheme_id", "State scheme must be a number");
//                rowErrors.put("row", String.valueOf(rowNum));
//                errors.add(rowErrors);
//            }
//        }
//    }
//
//    private String getCellValue(Cell cell) {
//        if (cell == null) return "";
//
//        switch (cell.getCellType()) {
//            case STRING:
//                return cell.getStringCellValue().trim();
//
//            case NUMERIC:
//                if (DateUtil.isCellDateFormatted(cell)) {
//                    return new DataFormatter().formatCellValue(cell).trim();
//                } else {
//                    return new BigDecimal(cell.getNumericCellValue())
//                            .toPlainString()
//                            .replaceAll("\\.0$", "");
//                }
//
//            case BOOLEAN:
//                return String.valueOf(cell.getBooleanCellValue()).trim();
//
//            case FORMULA:
//                return new DataFormatter().formatCellValue(cell).trim();
//
//            case BLANK:
//            default:
//                return "";
//        }
//    }
//

}
