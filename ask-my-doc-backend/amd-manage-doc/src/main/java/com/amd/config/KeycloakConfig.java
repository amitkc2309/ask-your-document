package com.amd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application.keycloak")
@Data
public class KeycloakConfig {
    private String realm;
    private String serverUrl;
    private String clientId;
    private String clientSecret;
}
