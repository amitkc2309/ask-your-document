package com.amd.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class KeycloakAuthFilter extends OncePerRequestFilter {

    private final KeycloakPolicyEnforcer enforcer;
    private final ResourceCache resourceCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        // JWT already validated by Spring Security
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String token = jwtAuth.getToken().getTokenValue();
        // Map request → resource
        String resource = resourceCache.getResource(request.getRequestURI());
        // Map method → scope
        String scope = request.getMethod(); // GET, POST, DELETE
        // Ask Keycloak
        boolean allowed = enforcer.isAllowed(token, resource, scope);
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/config/");
    }
}