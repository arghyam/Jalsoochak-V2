package org.arghyam.jalsoochak.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.dto.common.ApiResponseDTO;
import org.arghyam.jalsoochak.user.dto.internal.AuthResult;
import org.arghyam.jalsoochak.user.dto.request.ActivateAccountRequestDTO;
import org.arghyam.jalsoochak.user.dto.request.ForgotPasswordRequestDTO;
import org.arghyam.jalsoochak.user.dto.request.LoginRequestDTO;
import org.arghyam.jalsoochak.user.dto.request.ResetPasswordRequestDTO;
import org.arghyam.jalsoochak.user.dto.response.InviteInfoResponseDTO;
import org.arghyam.jalsoochak.user.dto.response.TokenResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.user.exceptions.BadRequestException;
import org.arghyam.jalsoochak.user.service.AuthService;
import org.arghyam.jalsoochak.user.util.CookieHelper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieHelper cookieHelper;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<TokenResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletResponse response) {
        AuthResult result = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieHelper.buildRefreshCookie(result.refreshToken(), result.refreshExpiresIn()).toString());
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Login successful", result.tokenResponse()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDTO<TokenResponseDTO>> refresh(
            @CookieValue(name = CookieHelper.REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token cookie is missing");
        }
        AuthResult result = authService.refreshToken(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieHelper.buildRefreshCookie(result.refreshToken(), result.refreshExpiresIn()).toString());
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Token refreshed", result.tokenResponse()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<Void>> logout(
            @CookieValue(name = CookieHelper.REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.clearRefreshCookie().toString());
            return ResponseEntity.ok(ApiResponseDTO.of(200, "Logged out successfully"));
        }
        try {
            authService.logout(refreshToken);
        } catch (Exception e) {
            log.error("Logout failed for session: {}", e.getMessage());
            response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.clearRefreshCookie().toString());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponseDTO.of(502, "Logout failed; your session may still be active on the server"));
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.clearRefreshCookie().toString());
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Logged out successfully"));
    }

    @GetMapping("/invite/info")
    public ResponseEntity<ApiResponseDTO<InviteInfoResponseDTO>> inviteInfo(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Invite info retrieved", authService.getInviteInfo(token)));
    }

    @PostMapping("/activate-account")
    public ResponseEntity<ApiResponseDTO<TokenResponseDTO>> activateAccount(
            @Valid @RequestBody ActivateAccountRequestDTO request,
            HttpServletResponse response) {
        AuthResult result = authService.activateAccount(request);
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieHelper.buildRefreshCookie(result.refreshToken(), result.refreshExpiresIn()).toString());
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Account activated successfully", result.tokenResponse()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "If this email is registered, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Password reset successfully"));
    }
}
