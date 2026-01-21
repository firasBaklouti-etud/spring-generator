package com.firas.generator.stack;

import com.firas.generator.model.FilePreview;
import com.firas.generator.model.ProjectRequest;

import java.io.IOException;
import java.util.List;

/**
 * Main interface for stack-specific project generation.
 * 
 * A StackProvider encapsulates all the knowledge and capabilities
 * needed to generate a complete project for a specific technology stack.
 * Each stack (Spring, Node, Nest, FastAPI) has its own implementation.
 * 
 * This interface follows the Strategy pattern, allowing the system to
 * switch between different stack generators at runtime based on user selection.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public interface StackProvider {
    
    // ==================== Identity ====================
    
    /**
     * @return The stack type this provider handles
     */
    StackType getStackType();
    
    /**
     * @return The template directory name (e.g., "spring", "node")
     */
    default String getTemplateDirectory() {
        return getStackType().getId();
    }
    
    // ==================== Project Generation ====================
    
    /**
     * Generates all project files and returns them as FilePreview objects.
     * This is used for the IDE preview feature.
     * 
     * @param request The project configuration
     * @return List of generated files with their content
     * @throws IOException If an error occurs during generation
     */
    List<FilePreview> generateProject(ProjectRequest request) throws IOException;
    
    /**
     * Generates the project and packages it as a ZIP file.
     * 
     * @param request The project configuration
     * @return Byte array containing the ZIP file
     * @throws IOException If an error occurs during generation or ZIP creation
     */
    byte[] generateProjectZip(ProjectRequest request) throws IOException;
    
    // ==================== Sub-Components ====================
    
    /**
     * @return The type mapper for converting SQL types to language types
     */
    TypeMapper getTypeMapper();
    
    /**
     * @return The code generator for producing CRUD code
     */
    CodeGenerator getCodeGenerator();
    
    /**
     * @return The dependency provider for this stack
     */
    DependencyProvider getDependencyProvider();
    
    // ==================== Utility Methods ====================
    
    /**
     * Applies type mapping to all columns in the given tables.
     * Converts SQL types (VARCHAR, BIGINT) to language-specific types.
     * 
     * @param request The project request containing tables
     */
    default void applyTypeMappings(ProjectRequest request) {
        if (request.getTables() == null) return;
        
        TypeMapper mapper = getTypeMapper();
        for (var table : request.getTables()) {
            for (var column : table.getColumns()) {
                if (column.getType() != null) {
                    column.setJavaType(mapper.mapSqlType(column.getType()));
                }
            }
        }
    }
}
