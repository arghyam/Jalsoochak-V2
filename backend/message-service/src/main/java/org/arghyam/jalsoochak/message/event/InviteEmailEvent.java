package org.arghyam.jalsoochak.message.event;

import lombok.Data;

@Data
public class InviteEmailEvent {

    private String eventType;
    private String to;
    private String name;
    private String role;
    private String inviteLink;
    private int expiryHours;
}
