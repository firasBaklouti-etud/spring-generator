package com.firas.generator.controller;

import com.firas.generator.model.DownloadRequest;
import com.firas.generator.model.FilePreview;
import com.firas.generator.model.ProjectRequest;
import com.firas.generator.model.ProjectPreviewResponse;
import com.firas.generator.service.ProjectGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * REST Controller for generating Spring Boot projects.
 * 
 * This controller handles project generation requests, creating customized Spring Boot
 * projects based on user specifications including dependencies, SQL schemas, and various
 * code generation options. The generated project is returned as a downloadable ZIP file.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-01
 */
@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow all for now
public class GeneratorController {

    /**
     * Service responsible for orchestrating the project generation process
     */
    private final ProjectGeneratorService projectGeneratorService;

    /**
     * Generates a complete Spring Boot project based on the provided configuration.
     * 
     * This endpoint accepts a project request containing:
     * - Basic project metadata (groupId, artifactId, name, description)
     * - Java and Spring Boot versions
     * - Selected dependencies
     * - Optional SQL schema for automatic CRUD generation
     * - Flags for including various code components (entities, repositories, services, controllers)
     * 
     * The generated project is returned as a ZIP file ready for download and extraction.
     * 
     * @param request The project configuration containing all generation parameters
     * @return ResponseEntity containing the ZIP file as byte array with appropriate headers
     * @throws IOException If an error occurs during project generation or ZIP creation
     */
    @PostMapping("/project")
    public ResponseEntity<byte[]> generateProject(@RequestBody ProjectRequest request) throws IOException {
        byte[] zipContent = projectGeneratorService.generateProject(request);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + request.getArtifactId() + ".zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipContent);
    }
    
    /**
     * Generates project files and returns them as JSON for preview/editing in the IDE interface.
     * 
     * This endpoint creates all project files (pom.xml, Java classes, properties files, etc.)
     * and returns them as a structured JSON response containing file paths, contents, and
     * detected programming languages for syntax highlighting.
     * 
     * @param request The project configuration containing all generation parameters
     * @return ResponseEntity containing the list of generated files as FilePreview objects
     * @throws IOException If an error occurs during project generation
     */
    @PostMapping("/preview")
    public ResponseEntity<ProjectPreviewResponse> previewProject(@RequestBody ProjectRequest request) throws IOException {
        List<FilePreview> files = projectGeneratorService.generateProjectPreview(request);
        return ResponseEntity.ok(new ProjectPreviewResponse(files));
    }
    
    /**
     * Creates a ZIP file from a list of file previews (potentially edited by the user).
     * 
     * This endpoint allows users to download their edited files as a complete project.
     * It accepts a list of files with their paths and contents, writes them to a
     * temporary directory structure, and returns them as a ZIP file.
     * 
     * @param request The download request containing files and artifact ID
     * @return ResponseEntity containing the ZIP file as byte array
     * @throws IOException If an error occurs during ZIP creation
     */
    @PostMapping("/from-files")
    public ResponseEntity<byte[]> generateProjectFromFiles(@RequestBody DownloadRequest request) throws IOException {
        byte[] zipContent = projectGeneratorService.generateZipFromFiles(request.getFiles(), request.getArtifactId());
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + request.getArtifactId() + ".zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipContent);
    }
    
}
