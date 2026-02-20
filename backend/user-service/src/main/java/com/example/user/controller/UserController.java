package com.example.user.controller;

import com.example.user.dto.request.InviteRequest;
import com.example.user.dto.request.LoginRequest;
import com.example.user.dto.request.RegisterRequest;
import com.example.user.dto.request.TokenRequest;
import com.example.user.dto.response.InviteToken;
import com.example.user.dto.response.TokenResponse;
import com.example.user.exceptions.BadRequestException;
import com.example.user.repository.UserCommonRepository;
import com.example.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/v2/auth", "/auth"})
@Slf4j
@Validated
public class UserController {
    private final UserService userService;
    private final UserCommonRepository userCommonRepository;

    public UserController(UserService userService, UserCommonRepository userCommonRepository) {
        this.userService = userService;
        this.userCommonRepository = userCommonRepository;
    }

    @PreAuthorize("hasRole('super_user')")
    @PostMapping("/invite/user")
    public ResponseEntity<InviteToken> inviteUser(
            @Valid @RequestBody InviteRequest inviteRequest,
            @RequestHeader("X-Tenant-Code") @NotBlank(message = "Tenant code is required") String tenantCode) {

        boolean tenantExist = userCommonRepository.existsTenantByStateCode(tenantCode);
        if (!tenantExist){
            log.warn("Invalid tenant code provided: {}", tenantCode);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid tenant code: " + tenantCode
            );
        }

        String inviteToken = userService.inviteUser(inviteRequest);
        String message = "Invite sent to " + inviteRequest.getEmail();
        return ResponseEntity.ok(new InviteToken(inviteToken, message));
    }

    @PostMapping("/complete/profile")
    public ResponseEntity<String> completeProfile(
            @Valid @RequestBody RegisterRequest registerRequest) {
        log.info("hit: {}", registerRequest);
        boolean tenantExist = userCommonRepository.existsTenantByStateCode(registerRequest.getTenantId());
        if (!tenantExist){
            log.warn("Invalid tenant code provided: {}", registerRequest.getTenantId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid tenant code: " + registerRequest.getTenantId()
            );
        }

        userService.completeProfile(registerRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               @RequestHeader("X-Tenant-Code") @NotBlank(message = "Tenant code is required") String tenantCode) {
        TokenResponse response = userService.login(request, tenantCode);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody TokenRequest tokenRequest) {
        TokenResponse response = userService.refreshToken(tokenRequest.getRefreshToken());
        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam String refreshToken) {
        boolean success = userService.logout(refreshToken);
        return success ? ResponseEntity.ok("Logged out") :
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Logout failed");
    }


//    @PostMapping("/bulk/invite")
//    public ResponseEntity<?> bulkInvite(
//            @RequestParam("file") MultipartFile file,
//            @RequestHeader("X-Tenant-Code") String tenantCode,
//            @RequestHeader(value = "Authorization", required = false) String authHeader) {
//
//        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("message", "Missing or invalid Authorization header"));
//        }
//
//        try {
//            if (!userCommonRepository.existsTenantByStateCode(tenantCode)) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(Map.of("message", "Invalid tenant code: " + tenantCode));
//            }
//
//            Map<String, Object> result = userService.bulkInviteUsers(file, tenantCode);
//            return ResponseEntity.ok(result);
//
//        } catch (BadRequestException e) {
//            log.warn("BadRequestException in bulkInvite: {}", e.getMessage());
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("message", e.getMessage());
//
//            if (e.getErrors() != null) {
//                response.put("errors", e.getErrors());
//            }
//
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(response);
//        } catch (Exception e) {
//            log.error("Error in bulkInvite", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("message", "An unexpected error occurred. Please contact support."));
//        }
//    }
}
