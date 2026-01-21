package com.firas.generator.controller;

import com.firas.generator.model.DependencyGroup;
import com.firas.generator.stack.StackProviderFactory;
import com.firas.generator.stack.StackType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}

