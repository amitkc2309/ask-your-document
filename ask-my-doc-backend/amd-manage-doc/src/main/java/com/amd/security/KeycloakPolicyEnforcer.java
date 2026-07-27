package com.amd.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Log
@RequiredArgsConstructor
public class KeycloakPolicyEnforcer {

    private final WebClient webClient;
    private final KeycloakUrlProvider keycloakUrlProvider;
    private final KeycloakPolicyCacheService keycloakPolicyCacheService;

    public boolean isAllowed(String token, String resource, String scope) {
        String username = SecurityUtils.getUsername();
        String cacheKey = buildCacheKey(username, resource, scope);
        Boolean cached = keycloakPolicyCacheService.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String url = keycloakUrlProvider.getTokenUrl();
        try {
            webClient.post()
                    .uri(url)
                    .headers(h -> h.setBearerAuth(token))
                    .body(BodyInserters.fromFormData("grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket")
                            .with("audience", keycloakUrlProvider.getClientId())
                            .with("permission", resource + "#" + scope))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            keycloakPolicyCacheService.put(cacheKey, true);
            return true;
        } catch (Exception ex) {
            log.severe(ex.getMessage());
            keycloakPolicyCacheService.put(cacheKey, false);
            return false;
        }
    }

    private String buildCacheKey(String username, String resource, String scope) {
        String version = keycloakPolicyCacheService.getPolicyVersion();
        return "kc_policy"+":"+username + ":" + resource + ":" + scope + ":" + version;
    }
}