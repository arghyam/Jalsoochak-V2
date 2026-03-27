package org.arghyam.jalsoochak.message.event;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = {"to", "resetLink"})
public class ResetPasswordEmailEvent {

    private String eventType;
    private String to;
    private String resetLink;
    private int expiryMinutes;
}
