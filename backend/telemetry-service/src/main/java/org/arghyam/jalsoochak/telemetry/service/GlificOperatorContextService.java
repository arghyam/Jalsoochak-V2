package org.arghyam.jalsoochak.telemetry.service;

import org.arghyam.jalsoochak.telemetry.repository.TenantConfigRepository;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryOperatorWithSchema;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryTenantRepository;
import org.arghyam.jalsoochak.telemetry.repository.UserLanguagePreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlificOperatorContextService {

    private final TelemetryTenantRepository telemetryTenantRepository;
    private final UserLanguagePreferenceRepository userLanguagePreferenceRepository;
    private final TenantConfigRepository tenantConfigRepository;

    public GlificOperatorContextService(TelemetryTenantRepository telemetryTenantRepository,
                                        UserLanguagePreferenceRepository userLanguagePreferenceRepository,
                                        TenantConfigRepository tenantConfigRepository) {
        this.telemetryTenantRepository = telemetryTenantRepository;
        this.userLanguagePreferenceRepository = userLanguagePreferenceRepository;
        this.tenantConfigRepository = tenantConfigRepository;
    }

    public TelemetryOperatorWithSchema resolveOperatorWithSchema(String contactId) {
        Integer preferredTenantId = userLanguagePreferenceRepository
                .findPreferredTenantIdByContactId(contactId)
                .orElse(null);

        return resolveOperatorWithSchema(contactId, preferredTenantId);
    }

    public TelemetryOperatorWithSchema resolveOperatorWithSchema(String contactId, Integer preferredTenantId) {
        return telemetryTenantRepository
                .findOperatorByPhoneAcrossTenants(contactId, preferredTenantId)
                .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + contactId));
    }

    public String resolveOperatorLanguage(TelemetryOperatorWithSchema operatorWithSchema, Integer tenantId) {
        if (operatorWithSchema == null || operatorWithSchema.operator() == null) {
            return "English";
        }
        Integer languageId = operatorWithSchema.operator().languageId();
        if (languageId == null || languageId <= 0) {
            return "English";
        }
        List<String> languageOptions = tenantConfigRepository.findLanguageOptions(tenantId);
        if (languageOptions.isEmpty() || languageId > languageOptions.size()) {
            return "English";
        }
        return languageOptions.get(languageId - 1);
    }
}
