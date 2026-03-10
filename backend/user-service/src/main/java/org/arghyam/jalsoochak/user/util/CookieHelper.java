package org.arghyam.jalsoochak.user.util;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.config.properties.CookieProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieHelper {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final CookieProperties cookieProperties;

    public ResponseCookie buildRefreshCookie(String token, long maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    public String cookieName() {
        return REFRESH_COOKIE_NAME;
    }
}
