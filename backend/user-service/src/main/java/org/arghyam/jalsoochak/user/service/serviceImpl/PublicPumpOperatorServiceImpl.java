package org.arghyam.jalsoochak.user.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorDetailsDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceRowDTO;
import org.arghyam.jalsoochak.user.repository.PublicPumpOperatorRepository;
import org.arghyam.jalsoochak.user.service.PublicPumpOperatorService;
import org.arghyam.jalsoochak.user.util.TenantSchemaResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicPumpOperatorServiceImpl implements PublicPumpOperatorService {

    private final PublicPumpOperatorRepository publicPumpOperatorRepository;

    @Override
    public PumpOperatorDetailsDTO getPumpOperatorDetails(String tenantCode, long pumpOperatorId) {
        String schemaName = TenantSchemaResolver.requireSchemaNameFromTenantCode(tenantCode);
        PumpOperatorDetailsDTO dto = publicPumpOperatorRepository.findPumpOperatorById(schemaName, pumpOperatorId);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pump operator not found");
        }
        return dto;
    }

    @Override
    public PumpOperatorReadingComplianceDTO getReadingCompliance(String tenantCode, long pumpOperatorId) {
        String schemaName = TenantSchemaResolver.requireSchemaNameFromTenantCode(tenantCode);
        PumpOperatorReadingComplianceDTO dto = publicPumpOperatorRepository.getReadingCompliance(schemaName, pumpOperatorId);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pump operator not found");
        }
        return dto;
    }

    @Override
    public List<PumpOperatorReadingComplianceRowDTO> listReadingCompliance(String tenantCode) {
        String schemaName = TenantSchemaResolver.requireSchemaNameFromTenantCode(tenantCode);
        return publicPumpOperatorRepository.listReadingCompliance(schemaName);
    }
}
