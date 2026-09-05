package com.ayd.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceCache {

    private final KeycloakResourceLoader loader;
    private Map<String, String> resourceMap = new HashMap<>();
    private final WebClient webClient;
    private final KeycloakUrlProvider keycloakUrlProvider;

    @PostConstruct
    public void init() {
        String token = getServiceToken();
        resourceMap = loader.loadResources(token);
        log.info("Resource Cache Loaded*****************"+resourceMap.entrySet());
    }

    public String getResource(String uri) {
        return resourceMap.entrySet().stream()
                .filter(e -> matches(uri, e.getKey()))
                .sorted((a, b) -> b.getKey().length() - a.getKey().length())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean matches(String uri, String pattern) {
        if (pattern.equals("/*")) {
            return true;
        }
        if (pattern.endsWith("/*")) {
            String base = pattern.substring(0, pattern.length() - 1); // remove *
            return uri.startsWith(base);
        }
        return uri.equals(pattern);
    }


    public String getServiceToken() {
        String url = keycloakUrlProvider.getTokenUrl();
        String body = "grant_type=client_credentials"
                + "&client_id="+keycloakUrlProvider.getClientId()
                + "&client_secret="+keycloakUrlProvider.getClientSecret();
        Map response = webClient.post()
                .uri(url)
                .headers(h -> h.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return (String) response.get("access_token");
    }
}
