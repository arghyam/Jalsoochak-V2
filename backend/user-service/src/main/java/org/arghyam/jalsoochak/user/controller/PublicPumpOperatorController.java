package org.arghyam.jalsoochak.user.controller;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.dto.common.ApiResponseDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorDetailsDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceRowDTO;
import org.arghyam.jalsoochak.user.service.PublicPumpOperatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pumpoperator")
@RequiredArgsConstructor
public class PublicPumpOperatorController {

    private final PublicPumpOperatorService publicPumpOperatorService;

    @GetMapping("/pump-operators/{pumpOperatorId}")
    public ResponseEntity<ApiResponseDTO<PumpOperatorDetailsDTO>> getPumpOperatorDetails(
            @PathVariable long pumpOperatorId,
            @RequestParam String tenantCode
    ) {
        PumpOperatorDetailsDTO dto = publicPumpOperatorService.getPumpOperatorDetails(tenantCode, pumpOperatorId);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Pump operator retrieved", dto));
    }

    @GetMapping("/pump-operators/{pumpOperatorId}/reading-compliance")
    public ResponseEntity<ApiResponseDTO<PumpOperatorReadingComplianceDTO>> getReadingCompliance(
            @PathVariable long pumpOperatorId,
            @RequestParam String tenantCode
    ) {
        PumpOperatorReadingComplianceDTO dto = publicPumpOperatorService.getReadingCompliance(tenantCode, pumpOperatorId);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Reading compliance retrieved", dto));
    }

    @GetMapping("/pump-operators/reading-compliance")
    public ResponseEntity<ApiResponseDTO<List<PumpOperatorReadingComplianceRowDTO>>> listReadingCompliance(
            @RequestParam String tenantCode
    ) {
        List<PumpOperatorReadingComplianceRowDTO> rows = publicPumpOperatorService.listReadingCompliance(tenantCode);
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Reading compliance retrieved", rows));
    }
}
