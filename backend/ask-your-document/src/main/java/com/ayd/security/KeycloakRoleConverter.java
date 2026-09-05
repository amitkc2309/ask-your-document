package com.ayd.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This is unused class;
 * In this project, we are using Keycloak-based Authorization.
 * Instead of parsing the JWT and validating roles (e.g., using @RolesAllowed), we intercept every request using a
 * custom filter (KeycloakAuthFilter).
 * The filter delegates the authorization decision to Keycloak via KeycloakPolicyEnforcer, which verifies
 * whether the request is allowed.
 * All authorization logic (policies and permissions) is configured on the Keycloak
 * side under Client → Authorization.
 */
@Component
public class KeycloakRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            roles.forEach(role ->
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }
        Map<String, Object> authorization = jwt.getClaim("authorization");
        if (authorization != null) {
            List<Map<String, Object>> permissions =
                    (List<Map<String, Object>>) authorization.get("permissions");
            if (permissions != null) {
                for (Map<String, Object> permission : permissions) {
                    List<String> scopes = (List<String>) permission.get("scopes");

                    if (scopes != null) {
                        scopes.forEach(scope ->
                                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
                    }
                }
            }
        }
        return new JwtAuthenticationToken(
                jwt,
                authorities,
                jwt.getClaim("preferred_username")
        );
    }
}