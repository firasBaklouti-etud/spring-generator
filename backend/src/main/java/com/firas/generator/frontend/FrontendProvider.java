package com.firas.generator.frontend;

import com.firas.generator.model.FilePreview;
import com.firas.generator.model.ProjectRequest;

import java.io.IOException;
import java.util.List;

/**
 * Interface for frontend project generation.
 * 
 * Each implementation handles a specific frontend framework (Next.js, Angular, React, etc.)
 * and generates the corresponding project files from the same ProjectRequest used for backend generation.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public interface FrontendProvider {
    
    /**
     * @return The framework identifier (e.g., "NEXTJS", "ANGULAR", "REACT")
     */
    String getFramework();
    
    /**
     * Generates all frontend project files.
     * 
     * @param request The project request containing tables, security config, etc.
     * @return List of generated files with paths prefixed with "frontend/"
     * @throws IOException If an error occurs during generation
     */
    List<FilePreview> generateFrontend(ProjectRequest request) throws IOException;
    
    /**
     * @return Whether this provider is available for use
     */
    boolean isAvailable();
}
