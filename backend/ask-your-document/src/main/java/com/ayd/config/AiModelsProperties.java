package com.ayd.config;

import com.ayd.enums.AiProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "application")
@Data
public class AiModelsProperties {
    private Map<AiProvider, List<String>> models = new EnumMap<>(AiProvider.class);
}
