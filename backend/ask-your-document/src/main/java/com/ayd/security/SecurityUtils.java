package com.ayd.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityUtils {

    /**
     * Get the authenticated Jwt token from the security context.
     *
     * @return the Jwt token or null if not authenticated
     */
    public static Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            return (Jwt) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Get the authenticated userId from the security context.
     *
     * @return the userId or null if not authenticated
     */
    public static String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Get any claim from the Jwt token.
     *
     * @param claimName the name of the claim to retrieve
     * @return the claim value or null if not present
     */
    public static String getClaimAsString(String claimName) {
        Jwt jwt = getJwt();
        if (jwt != null) {
            return jwt.getClaimAsString(claimName);
        }
        return null;
    }
}

