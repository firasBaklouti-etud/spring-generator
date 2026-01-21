package com.firas.generator.controller;

import com.firas.generator.stack.StackProviderFactory;
import com.firas.generator.stack.StackType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for discovering available technology stacks.
 * 
 * This controller provides endpoints to list all supported technology stacks
 * (Spring, Node, Nest, FastAPI, etc.) and their metadata.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
@RestController
@RequestMapping("/api/stacks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StackController {
    
    private final StackProviderFactory stackProviderFactory;
    
    /**
     * Returns a list of all available technology stacks.
     * 
     * @return List of stack information objects
     */
    @GetMapping
    public List<StackInfo> getAvailableStacks() {
        return stackProviderFactory.getAvailableStackTypes().stream()
                .map(type -> new StackInfo(
                        type.getId(),
                        type.getDisplayName(),
                        type.getLanguage(),
                        type.getDefaultVersion()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * Returns information about a specific stack.
     * 
     * @param stackId The stack identifier (e.g., "spring", "node")
     * @return Stack information
     */
    @GetMapping("/{stackId}")
    public StackInfo getStack(@PathVariable String stackId) {
        StackType type = StackType.fromId(stackId);
        if (!stackProviderFactory.hasProvider(type)) {
            throw new IllegalArgumentException("Stack not available: " + stackId);
        }
        return new StackInfo(
                type.getId(),
                type.getDisplayName(),
                type.getLanguage(),
                type.getDefaultVersion()
        );
    }
    
    /**
     * DTO for stack information.
     */
    public record StackInfo(
            String id,
            String displayName,
            String language,
            String defaultVersion
    ) {}
}
