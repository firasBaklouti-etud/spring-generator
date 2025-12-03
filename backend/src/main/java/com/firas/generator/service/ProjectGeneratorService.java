package com.firas.generator.service;

import com.firas.generator.model.FilePreview;
import com.firas.generator.model.ProjectRequest;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service interface for generating Spring Boot projects.
 * 
 * This service is responsible for orchestrating the entire project generation process,
 * including creating the project structure, generating configuration files, and
 * optionally generating CRUD code from SQL schemas.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-01
 */
public interface ProjectGeneratorService {
    /**
     * Generates a complete Spring Boot project based on the provided request.
     * 
     * @param request The project configuration containing all generation parameters
     * @return Byte array containing the generated project as a ZIP file
     * @throws IOException If an error occurs during project generation or ZIP creation
     */
    byte[] generateProject(ProjectRequest request) throws IOException;
    
    /**
     * Generates project files and returns them as a list for preview/editing.
     * 
     * @param request The project configuration containing all generation parameters
     * @return List of FilePreview objects containing paths and contents
     * @throws IOException If an error occurs during project generation
     */
    List<FilePreview> generateProjectPreview(ProjectRequest request) throws IOException;
    
    /**
     * Creates a ZIP file from a list of file previews (potentially edited by the user).
     * 
     * @param files List of files with paths and contents
     * @param artifactId Project artifact ID for naming the ZIP
     * @return Byte array containing the ZIP file
     * @throws IOException If an error occurs during ZIP creation
     */
    byte[] generateZipFromFiles(List<FilePreview> files, String artifactId) throws IOException;
}

