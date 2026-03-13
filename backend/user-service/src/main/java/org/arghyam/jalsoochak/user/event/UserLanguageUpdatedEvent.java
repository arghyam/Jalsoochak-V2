package org.arghyam.jalsoochak.user.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLanguageUpdatedEvent {
    private String eventType;   // USER_LANGUAGE_UPDATED
    private Integer tenantId;
    private String phoneNumber;
    private Integer languageId;
    private String source;      // e.g. CSV_ONBOARDED
}
