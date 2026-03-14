package org.arghyam.jalsoochak.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.dto.common.ApiResponseDTO;
import org.arghyam.jalsoochak.user.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.user.exceptions.BadRequestException;
import org.arghyam.jalsoochak.user.dto.request.ChangePasswordRequestDTO;
import org.arghyam.jalsoochak.user.dto.request.InviteRequestDTO;
import org.arghyam.jalsoochak.user.dto.request.UpdateProfileRequestDTO;
import org.arghyam.jalsoochak.user.dto.response.AdminUserResponseDTO;
import org.arghyam.jalsoochak.user.service.UserManagementService;
import org.arghyam.jalsoochak.user.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;

    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('SUPER_USER', 'STATE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> inviteUser(@Valid @RequestBody InviteRequestDTO request,
                                                           Authentication authentication) {
        userManagementService.inviteUser(request, authentication);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Invitation sent successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<AdminUserResponseDTO>> getMe(Authentication authentication) {
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Profile retrieved",
                userManagementService.getMe(SecurityUtils.getKeycloakId(authentication))));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponseDTO<AdminUserResponseDTO>> updateMe(Authentication authentication,
                                                                         @Valid @RequestBody UpdateProfileRequestDTO request) {
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Profile updated",
                userManagementService.updateMe(SecurityUtils.getKeycloakId(authentication), request)));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponseDTO<Void>> changePassword(Authentication authentication,
                                                               @Valid @RequestBody ChangePasswordRequestDTO request) {
        userManagementService.changePassword(SecurityUtils.getKeycloakId(authentication), request);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Password changed successfully"));
    }

    @GetMapping("/super-users")
    @PreAuthorize("hasRole('SUPER_USER')")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<AdminUserResponseDTO>>> listSuperUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        if (page < 0) throw new BadRequestException("page must be >= 0");
        if (limit < 1 || limit > 100) throw new BadRequestException("limit must be between 1 and 100");
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Super users retrieved",
                userManagementService.listSuperUsers(page, limit)));
    }

    @GetMapping("/state-admins")
    @PreAuthorize("hasAnyRole('SUPER_USER', 'STATE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<AdminUserResponseDTO>>> listStateAdmins(
            @RequestParam(required = false) String tenantCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        if (page < 0) throw new BadRequestException("page must be >= 0");
        if (limit < 1 || limit > 100) throw new BadRequestException("limit must be between 1 and 100");
        return ResponseEntity.ok(ApiResponseDTO.of(200, "State admins retrieved",
                userManagementService.listStateAdmins(tenantCode, authentication, page, limit)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_USER', 'STATE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<AdminUserResponseDTO>> getUserById(@PathVariable Long id,
                                                                           Authentication authentication) {
        return ResponseEntity.ok(ApiResponseDTO.of(200, "User retrieved", userManagementService.getUserById(id, authentication)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_USER')")
    public ResponseEntity<ApiResponseDTO<AdminUserResponseDTO>> updateUserById(@PathVariable Long id,
                                                                               Authentication authentication,
                                                                               @Valid @RequestBody UpdateProfileRequestDTO request) {
        return ResponseEntity.ok(ApiResponseDTO.of(200, "User updated", userManagementService.updateUserById(id, authentication, request)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_USER', 'STATE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> deactivate(@PathVariable Long id, Authentication authentication) {
        userManagementService.deactivateUser(id, authentication);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "User deactivated successfully"));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_USER', 'STATE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> activate(@PathVariable Long id, Authentication authentication) {
        userManagementService.activateUser(id, authentication);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "User activated successfully"));
    }

    @PostMapping("/{id}/reinvite")
    @PreAuthorize("hasAnyRole('SUPER_USER', 'STATE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> reinvite(@PathVariable Long id, Authentication authentication) {
        userManagementService.reinviteUser(id, authentication);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Invitation resent successfully"));
    }
}
