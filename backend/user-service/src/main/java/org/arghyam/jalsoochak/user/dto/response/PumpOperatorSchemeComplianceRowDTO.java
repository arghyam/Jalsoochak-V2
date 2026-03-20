package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;
import org.arghyam.jalsoochak.user.enums.TenantUserStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record PumpOperatorSchemeComplianceRowDTO(
        Long id,
        String uuid,
        String name,
        String email,
        String phoneNumber,
        TenantUserStatus status,
        Long schemeId,
        String schemeName,
        Integer schemeMappingStatus,
        LocalDate onboardingDate,
        Integer totalActiveDays,
        Integer submittedDays,
        Integer missedSubmissionDays,
        Integer inactiveDays,
        Integer missingSubmissionCount,
        BigDecimal reportingRatePercent,
        LocalDate readingDate,
        LocalDateTime readingAt,
        LocalDateTime lastSubmissionAt,
        BigDecimal confirmedReading
) {
}
