package com.firas.generator.controller;

import com.firas.generator.model.DependencyGroup;
import com.firas.generator.stack.StackProviderFactory;
import com.firas.generator.stack.StackType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for managing dependencies for all technology stacks.
 * 
 * This controller provides endpoints to retrieve available dependencies
 * grouped by categories. It supports multiple stacks (Spring, Node, etc.)
 * via the optional stackType parameter.
 * 
 * For backward compatibility, if no stackType is specified, it defaults to SPRING.
 * 
 * @author Firas Baklouti
 * @version 2.0
 * @since 2025-12-01
 */
@RestController
@RequestMapping("/api/dependencies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DependencyController {

    /**
     * Factory for retrieving stack-specific providers
     */
    private final StackProviderFactory stackProviderFactory;

    /**
     * Retrieves all available dependency groups for the specified stack.
     * 
     * Each group contains related dependencies (e.g., all web-related dependencies
     * are grouped together). This endpoint is typically used to populate dependency
     * selection UI in the frontend.
     * 
     * @param stackType The technology stack (defaults to SPRING for backward compatibility)
     * @return List of dependency groups, each containing multiple dependencies
     */
    @GetMapping("/groups")
    public List<DependencyGroup> getDependencyGroups(
            @RequestParam(required = false, defaultValue = "SPRING") StackType stackType) {
        return stackProviderFactory.getProvider(stackType)
                .getDependencyProvider()
                .getAllGroups();
    }

    /**
     * Retrieves recommended dependency IDs based on project configuration.
     * 
     * This endpoint returns a list of dependency IDs that should be automatically
     * selected based on the user's choices (database type, security settings, etc.).
     * Dependencies are still fetched from the backend's registry.
     * 
     * @param stackType The technology stack (defaults to SPRING)
     * @param databaseType The selected database type (mysql, postgresql, mariadb, sqlite, sqlserver)
     * @param securityEnabled Whether security is enabled
     * @param securityType The authentication type (BASIC, JWT, OAUTH2)
     * @return List of recommended dependency IDs
     */
    @GetMapping("/recommended")
    public List<String> getRecommendedDependencies(
            @RequestParam(required = false, defaultValue = "SPRING") StackType stackType,
            @RequestParam(required = false) String databaseType,
            @RequestParam(required = false, defaultValue = "false") boolean securityEnabled,
            @RequestParam(required = false) String securityType) {
        
        List<String> recommendedIds = new ArrayList<>();
        
        if (stackType == StackType.SPRING) {
            // Always recommend Spring Web for REST APIs
            recommendedIds.add("web");
            
            // Always recommend Spring Data JPA for database access
            recommendedIds.add("data-jpa");
            
            // Database-specific dependencies
            if (databaseType != null) {
                switch (databaseType.toLowerCase()) {
                    case "mysql":
                        recommendedIds.add("mysql");
                        break;
                    case "postgresql":
                        recommendedIds.add("postgresql");
                        break;
                    case "mariadb":
                        recommendedIds.add("mariadb");
                        break;
                    case "sqlserver":
                        recommendedIds.add("sqlserver");
                        break;
                    case "h2":
                        recommendedIds.add("h2");
                        break;
                    // SQLite uses a different driver, not from Spring Initializr
                }
            }
            
            // Security dependencies
            if (securityEnabled) {
                recommendedIds.add("security");
                
                // JWT-specific dependencies (handled via pom.xml template)
                if ("JWT".equalsIgnoreCase(securityType)) {
                    recommendedIds.add("validation");
                }
            }
            
            // Common useful dependencies
            recommendedIds.add("lombok");
            recommendedIds.add("devtools");
        }
        
        // Filter to only include IDs that exist in the dependency registry
        List<DependencyGroup> groups = stackProviderFactory.getProvider(stackType)
                .getDependencyProvider()
                .getAllGroups();
        
        List<String> validIds = new ArrayList<>();
        for (String id : recommendedIds) {
            for (DependencyGroup group : groups) {
                if (group.getDependencies().stream().anyMatch(d -> d.getId().equals(id))) {
                    validIds.add(id);
                    break;
                }
            }
        }
        
        return validIds;
    }
}

