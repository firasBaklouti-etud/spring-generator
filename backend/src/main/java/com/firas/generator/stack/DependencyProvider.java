package com.firas.generator.stack;

import com.firas.generator.model.DependencyGroup;

import java.util.List;

/**
 * Interface for providing stack-specific dependencies.
 * 
 * Each stack implementation has its own dependency source:
 * - Spring: Fetches from start.spring.io API
 * - Node/Nest: NPM packages
 * - FastAPI: PyPI packages
 * 
 * This abstraction allows the dependency controller to serve
 * dependencies for any supported stack.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public interface DependencyProvider {
    
    /**
     * Retrieves all available dependency groups for this stack.
     * 
     * @return List of dependency groups with their associated dependencies
     */
    List<DependencyGroup> getAllGroups();
    
    /**
     * Checks if this provider has been initialized with dependencies.
     * 
     * @return true if dependencies are loaded and available
     */
    boolean isInitialized();
    
    /**
     * Refreshes the dependency cache by re-fetching from the source.
     */
    void refresh();
}
