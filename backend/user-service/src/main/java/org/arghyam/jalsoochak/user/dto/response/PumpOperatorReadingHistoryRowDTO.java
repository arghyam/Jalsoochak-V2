package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record PumpOperatorReadingHistoryRowDTO(
        LocalDate readingDate,
        LocalDateTime readingAt,
        BigDecimal confirmedReading
) {
}
