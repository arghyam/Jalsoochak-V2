package org.arghyam.jalsoochak.tenant.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@UtilityClass
public class SecurityUtils {

    /**
     * Gets the current user's UUID (subject) from the JWT in SecurityContext.
     */
    public static String getCurrentUserUuid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }

    /**
     * Gets the current user's name from the JWT.
     * Returns "System" if no authentication is present or name claim is null.
     */
    public static String getCurrentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String name = jwt.getClaimAsString("name");
            if (name != null) {
                return name;
            }
        }
        return "System";
    }
}
