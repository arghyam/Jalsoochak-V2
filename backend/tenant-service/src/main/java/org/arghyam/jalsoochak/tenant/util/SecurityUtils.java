package org.arghyam.jalsoochak.tenant.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecurityUtils {

    /**
     * Gets the current user's UUID (subject) from the JWT in SecurityContext.
     */
    public static String getCurrentUserUuid() {
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
        //     return jwt.getSubject();
        // }
        return null;
    }

    /**
     * Gets the current user's display name from the JWT.
     * Fallback chain: "name" (full name) → "preferred_username" (always present in Keycloak)
     * → subject UUID → "System" (unauthenticated/system context).
     * If "name" is absent in your realm, ensure the "Full Name" mapper is enabled on the client,
     * or rely on "preferred_username" which Keycloak maps by default.
     */
    public static String getCurrentUserName() {
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
        //     String name = jwt.getClaimAsString("name");
        //     if (name != null) return name;
        //     String preferredUsername = jwt.getClaimAsString("preferred_username");
        //     if (preferredUsername != null) return preferredUsername;
        //     String sub = jwt.getSubject();
        //     if (sub != null) return sub;
        // }
        return "System";
    }
}