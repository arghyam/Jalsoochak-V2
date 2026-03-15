package org.arghyam.jalsoochak.user.service;

import org.arghyam.jalsoochak.user.dto.response.PumpOperatorDetailsDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceRowDTO;
import org.arghyam.jalsoochak.user.dto.response.SchemePumpOperatorsDTO;

import java.util.List;

public interface PublicPumpOperatorService {

    PumpOperatorDetailsDTO getPumpOperatorDetails(String tenantCode, long pumpOperatorId);

    PumpOperatorReadingComplianceDTO getReadingCompliance(String tenantCode, long pumpOperatorId);

    List<PumpOperatorReadingComplianceRowDTO> listReadingCompliance(String tenantCode);

    List<SchemePumpOperatorsDTO> listPumpOperatorsByScheme(String tenantCode, List<Long> schemeIds);
}
