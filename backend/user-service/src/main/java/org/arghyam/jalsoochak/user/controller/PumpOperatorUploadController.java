package org.arghyam.jalsoochak.user.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.user.config.TenantContext;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorUploadResponseDTO;
import org.arghyam.jalsoochak.user.repository.UserCommonRepository;
import org.arghyam.jalsoochak.user.service.PumpOperatorUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PumpOperatorUploadController {

    private final PumpOperatorUploadService pumpOperatorUploadService;
    private final UserCommonRepository userCommonRepository;

    @PostMapping(value = "/pump-operators/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PumpOperatorUploadResponseDTO> uploadPumpOperatorMappings(
            @RequestHeader("X-Tenant-Code") @NotBlank(message = "Tenant code is required") String tenantCode,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam("file") MultipartFile file
    ) {
        if (!userCommonRepository.existsTenantByStateCode(tenantCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenant code: " + tenantCode);
        }
        // Make tenant selection explicit (TenantInterceptor also sets this from X-Tenant-Code).
        TenantContext.setSchema("tenant_" + tenantCode.toLowerCase().trim());
        log.info("POST /api/pump-operators/upload called with file: {}", file != null ? file.getOriginalFilename() : null);
        return ResponseEntity.ok(pumpOperatorUploadService.uploadPumpOperatorMappings(file, authorizationHeader));
    }
}
