package org.arghyam.jalsoochak.user.controller;

import org.arghyam.jalsoochak.user.dto.request.InviteRequest;
import org.arghyam.jalsoochak.user.dto.request.LoginRequest;
import org.arghyam.jalsoochak.user.dto.request.RegisterRequest;
import org.arghyam.jalsoochak.user.dto.request.TokenRequest;
import org.arghyam.jalsoochak.user.dto.response.InviteToken;
import org.arghyam.jalsoochak.user.dto.response.TokenResponse;
import org.arghyam.jalsoochak.user.repository.UserCommonRepository;
import org.arghyam.jalsoochak.user.service.UserService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

        userService.inviteUser(inviteRequest);
        String message = "Invite sent to " + inviteRequest.getEmail();
        return ResponseEntity.ok(new InviteToken(message));
    }

    @PostMapping("/complete/profile")
    public ResponseEntity<String> completeProfile(
            @Valid @RequestBody RegisterRequest registerRequest) {
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
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody TokenRequest tokenRequest) {
        TokenResponse response = userService.refreshToken(tokenRequest.getRefreshToken());
        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody TokenRequest tokenRequest) {
        boolean success = userService.logout(tokenRequest.getRefreshToken());
        return success ? ResponseEntity.ok("Logged out") :
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Logout failed");
    }
}
