package com.ayd.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeycloakResourceLoader {

    private final WebClient webClient;
    private final KeycloakUrlProvider keycloakUrlProvider;

    public Map<String, String> loadResources(String token) {
        String baseUrl = keycloakUrlProvider.resourceSetUrl();
        String[] response =  webClient.get()
                .uri(baseUrl)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(String[].class)
                .block();
        Map<String, String> map = new HashMap<>();
        for (String id : response) {
            ResourceRepresentation resource =
                    webClient.get()
                            .uri(baseUrl + "/" + id)
                            .headers(h -> h.setBearerAuth(token))
                            .retrieve()
                            .bodyToMono(ResourceRepresentation.class)
                            .block();
            if (resource.getUris() != null) {
                for (String uri : resource.getUris()) {
                    map.put(uri, resource.getName());
                }
            }
        }

        return map;
    }
}