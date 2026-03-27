package org.arghyam.jalsoochak.user.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InviteEmailEvent {

    private String eventType;
    private String to;
    private String name;
    private String role;
    private String inviteLink;
    private int expiryHours;
}
