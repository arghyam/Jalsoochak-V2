package org.arghyam.jalsoochak.tenant.controller;

import java.util.Set;

import org.arghyam.jalsoochak.tenant.dto.ApiResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.SetSystemConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.SystemConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.enums.SystemConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.service.SystemManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for platform-wide system configurations.
 * These settings affect the entire system and are managed only by Super
 * Admins.
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System Configuration", description = "Endpoints for managing global system settings")
public class SystemController {

    private final SystemManagementService systemManagementService;

    /**
     * 1. API for getting system-wide configurations - accessible only by super
     * admin
     */
    @Operation(summary = "Get system configurations", description = "Retrieves global platform settings. Super Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System configurations retrieved successfully", content = @Content(schema = @Schema(implementation = SystemConfigResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden to access this resource"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/config")
    // TODO: Change this to permission / role based authorization for SUPER_ADMIN
    // @PreAuthorize("hasAuthority('tenant.create')")
    @PreAuthorize("permitAll")
    public ResponseEntity<ApiResponseDTO<SystemConfigResponseDTO>> getSystemConfigs(
            @RequestParam(required = false) Set<SystemConfigKeyEnum> keys) {
        log.info("GET /api/v1/system/config [keys={}]", keys);
        SystemConfigResponseDTO response = systemManagementService.getSystemConfigs(keys);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "System configurations retrieved successfully", response));
    }

    /**
     * 2. API for updating system-wide configurations - accessible only by super
     * admin
     */
    @Operation(summary = "Set system configurations", description = "Updates global system settings. Super Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System configurations set successfully", content = @Content(schema = @Schema(implementation = SystemConfigResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden to access this resource"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PutMapping("/config")
    // TODO: Change this to permission / role based authorization for SUPER_ADMIN
    // @PreAuthorize("hasAuthority('tenant.create') or
    // hasAuthority('tenant.update')")
    @PreAuthorize("permitAll")
    public ResponseEntity<ApiResponseDTO<SystemConfigResponseDTO>> setSystemConfigs(
            @Valid @RequestBody SetSystemConfigRequestDTO request) {
        log.info("PUT /api/v1/system/config");
        SystemConfigResponseDTO response = systemManagementService.setSystemConfigs(request);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "System configurations set successfully", response));
    }
}
