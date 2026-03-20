package org.arghyam.jalsoochak.user.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResetPasswordEmailEvent {

    private String eventType;
    private String to;
    private String resetLink;
    private int expiryMinutes;
}
