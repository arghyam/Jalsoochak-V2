package org.arghyam.jalsoochak.user.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "invite")
public record InviteProperties(int expiryHours) {}
