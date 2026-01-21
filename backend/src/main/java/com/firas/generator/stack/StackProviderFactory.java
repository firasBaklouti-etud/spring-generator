package com.firas.generator.stack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating and managing StackProvider instances.
 * 
 * This factory automatically discovers all StackProvider implementations
 * via Spring dependency injection and provides methods to retrieve them
 * by stack type.
 * 
 * Uses the Factory pattern to centralize provider creation and lookup,
 * similar to the existing AIProviderFactory pattern.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
@Component
public class StackProviderFactory {
    
    /** Map of stack type to provider implementation */
    private final Map<StackType, StackProvider> providers;
    
    /**
     * Constructs the factory with all available StackProvider implementations.
     * Spring automatically injects all beans implementing StackProvider.
     * 
     * @param providerList All discovered StackProvider implementations
     */
    @Autowired
    public StackProviderFactory(List<StackProvider> providerList) {
        this.providers = providerList.stream()
            .collect(Collectors.toMap(
                StackProvider::getStackType,
                Function.identity()
            ));
    }
    
    /**
     * Retrieves a StackProvider for the specified stack type.
     * 
     * @param stackType The desired stack type
     * @return The corresponding StackProvider
     * @throws IllegalArgumentException if no provider exists for the stack type
     */
    public StackProvider getProvider(StackType stackType) {
        StackProvider provider = providers.get(stackType);
        if (provider == null) {
            throw new IllegalArgumentException(
                "No provider available for stack: " + stackType + 
                ". Available stacks: " + getAvailableStackTypes()
            );
        }
        return provider;
    }
    
    /**
     * Retrieves a StackProvider by stack ID string.
     * 
     * @param stackId The stack identifier (e.g., "spring", "node")
     * @return The corresponding StackProvider
     * @throws IllegalArgumentException if no provider exists for the stack ID
     */
    public StackProvider getProviderById(String stackId) {
        return getProvider(StackType.fromId(stackId));
    }
    
    /**
     * @return The default StackProvider (SPRING)
     */
    public StackProvider getDefaultProvider() {
        return getProvider(StackType.SPRING);
    }
    
    /**
     * @return List of all available stack types that have providers
     */
    public List<StackType> getAvailableStackTypes() {
        return List.copyOf(providers.keySet());
    }
    
    /**
     * Checks if a provider exists for the given stack type.
     * 
     * @param stackType The stack type to check
     * @return true if a provider is available
     */
    public boolean hasProvider(StackType stackType) {
        return providers.containsKey(stackType);
    }
    
    /**
     * @return Number of available stack providers
     */
    public int getProviderCount() {
        return providers.size();
    }
}
