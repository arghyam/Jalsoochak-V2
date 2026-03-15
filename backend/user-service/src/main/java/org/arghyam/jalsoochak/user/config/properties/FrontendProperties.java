package org.arghyam.jalsoochak.user.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend")
public record FrontendProperties(String baseUrl, String invitePath, String resetPath) {
    public FrontendProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("frontend.base-url must be set");
        }
        baseUrl = baseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }
}
