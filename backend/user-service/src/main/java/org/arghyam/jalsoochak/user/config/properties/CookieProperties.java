package org.arghyam.jalsoochak.user.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cookie")
public record CookieProperties(boolean secure, String sameSite) {}
