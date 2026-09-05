package com.ayd.security;

import com.ayd.config.KeycloakConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KeycloakUrlProvider {
    private final KeycloakConfig keycloakConfig;

    public String getBaseRealmUrl() {
        return keycloakConfig.getServerUrl() + "/realms/" + getRealm();
    }

    public String getRealm(){
        return keycloakConfig.getRealm();
    }

    public String getClientId(){
        return keycloakConfig.getClientId();
    }

    public String getClientSecret(){
        return keycloakConfig.getClientSecret();
    }

    public String getTokenUrl() {
        return getBaseRealmUrl() + "/protocol/openid-connect/token";
    }

    public String resourceSetUrl() {
        return getBaseRealmUrl() + "/authz/protection/resource_set";
    }
}
