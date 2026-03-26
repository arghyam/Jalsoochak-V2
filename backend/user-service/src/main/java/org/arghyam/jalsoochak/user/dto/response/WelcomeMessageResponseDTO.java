package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;

@Builder
public record WelcomeMessageResponseDTO(
        int totalCandidates,
        int totalPhones,
        int batches,
        int batchSize,
        int publishedEvents,
        String message
) {
}
