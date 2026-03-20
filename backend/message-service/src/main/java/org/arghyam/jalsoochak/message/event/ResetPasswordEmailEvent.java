package org.arghyam.jalsoochak.message.event;

import lombok.Data;

@Data
public class ResetPasswordEmailEvent {

    private String eventType;
    private String to;
    private String resetLink;
    private int expiryMinutes;
}
