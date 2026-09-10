package com.ayd.factory;

import com.ayd.enums.AiProvider;
import com.ayd.strategy.AiProviderStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiProviderFactory {

    private final Map<AiProvider, AiProviderStrategy> strategies;

    // Spring automatically discovers and injects all AiProviderStrategy implementations
    public AiProviderFactory(List<AiProviderStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(AiProviderStrategy::getProvider, Function.identity()));
    }

    public AiProviderStrategy getStrategy(AiProvider provider) {
        AiProviderStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported AI Provider: " + provider);
        }
        return strategy;
    }
}
