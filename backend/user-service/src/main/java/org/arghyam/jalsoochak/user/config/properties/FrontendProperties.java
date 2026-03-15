package org.arghyam.jalsoochak.user.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend")
public record FrontendProperties(String baseUrl, String invitePath, String resetPath) {
    public FrontendProperties {
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }
}
