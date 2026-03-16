package org.arghyam.jalsoochak.scheme.controller;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.scheme.dto.SchemeDTO;
import org.arghyam.jalsoochak.scheme.repository.SchemeDbRepository;
import org.arghyam.jalsoochak.scheme.util.TenantSchemaResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicSchemeController {

    private final SchemeDbRepository schemeDbRepository;

    @GetMapping("/schemes/{schemeId}")
    public ResponseEntity<SchemeDTO> getSchemeDetails(
            @PathVariable int schemeId,
            @RequestParam String tenantCode
    ) {
        String schemaName = TenantSchemaResolver.requireSchemaNameFromTenantCode(tenantCode);
        SchemeDTO dto = schemeDbRepository.findSchemeById(schemaName, schemeId);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheme not found");
        }
        return ResponseEntity.ok(dto);
    }
}

