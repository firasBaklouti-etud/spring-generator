package com.firas.generator.frontend;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory for frontend providers. Auto-discovers all FrontendProvider beans via Spring DI.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
@Component
public class FrontendProviderFactory {
    
    private final Map<String, FrontendProvider> providers;
    
    public FrontendProviderFactory(List<FrontendProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(FrontendProvider::getFramework, p -> p));
    }
    
    /**
     * Get a provider by framework name.
     */
    public FrontendProvider getProvider(String framework) {
        FrontendProvider provider = providers.get(framework.toUpperCase());
        if (provider == null) {
            throw new IllegalArgumentException("No frontend provider found for framework: " + framework);
        }
        return provider;
    }
    
    /**
     * Check if a provider exists for the given framework.
     */
    public boolean hasProvider(String framework) {
        return providers.containsKey(framework.toUpperCase());
    }
    
    /**
     * Get list of available framework names.
     */
    public List<String> getAvailableFrameworks() {
        return providers.entrySet().stream()
                .filter(e -> e.getValue().isAvailable())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
