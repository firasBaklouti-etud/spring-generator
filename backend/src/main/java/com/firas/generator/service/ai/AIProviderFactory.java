package com.firas.generator.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AIProviderFactory {
    private final Map<String, AIProvider> providers;

    @Autowired
    public AIProviderFactory(List<AIProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        AIProvider::getProviderName,
                        Function.identity()
                ));
    }

    /**
     * Get a provider by name
     */
    public AIProvider getProvider(String providerName) {
        AIProvider provider = providers.get(providerName.toUpperCase());
        if (provider == null) {
            throw new IllegalArgumentException("Unknown AI provider: " + providerName);
        }
        if (!provider.isAvailable()) {
            throw new IllegalStateException("AI provider " + providerName + " is not available");
        }
        return provider;
    }

    /**
     * Get the default provider (first available)
     */
    public AIProvider getDefaultProvider() {
        return providers.values().stream()
                .filter(AIProvider::isAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No AI providers available"));
    }

    /**
     * Get all available provider names
     */
    public List<String> getAvailableProviders() {
        return providers.values().stream()
                .filter(AIProvider::isAvailable)
                .map(AIProvider::getProviderName)
                .collect(Collectors.toList());
    }
}