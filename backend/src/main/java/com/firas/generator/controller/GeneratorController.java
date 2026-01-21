package com.firas.generator.controller;

import com.firas.generator.model.DownloadRequest;
import com.firas.generator.model.FilePreview;
import com.firas.generator.model.ProjectRequest;
import com.firas.generator.model.ProjectPreviewResponse;
import com.firas.generator.stack.StackProvider;
import com.firas.generator.stack.StackProviderFactory;
import com.firas.generator.stack.StackType;
import com.firas.generator.util.ZipUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * REST Controller for generating projects for multiple technology stacks.
 * 
 * This controller handles project generation requests, creating customized projects
 * based on user specifications including stack type, dependencies, SQL schemas, and various
 * code generation options. The generated project is returned as a downloadable ZIP file.
 * 
 * Supports multiple stacks (Spring, Node, Nest, FastAPI) via the stackType field in the request.
 * For backward compatibility, if no stackType is specified, it defaults to SPRING.
 * 
 * @author Firas Baklouti
 * @version 3.0
 * @since 2025-12-01
 */
@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GeneratorController {

    /**
     * Factory for retrieving stack-specific providers
     */
    private final StackProviderFactory stackProviderFactory;

    /**
     * Generates a complete project based on the provided configuration.
     * 
     * This endpoint accepts a project request containing:
     * - Stack type (defaults to SPRING)
     * - Basic project metadata (name, description, packageName)
     * - Stack-specific config (springConfig, nodeConfig, etc.)
     * - Selected dependencies
     * - Optional table metadata for automatic CRUD generation
     * - Flags for including various code components
     * 
     * The generated project is returned as a ZIP file ready for download and extraction.
     * 
     * @param request The project configuration containing all generation parameters
     * @return ResponseEntity containing the ZIP file as byte array with appropriate headers
     * @throws IOException If an error occurs during project generation or ZIP creation
     */
    @PostMapping("/project")
    public ResponseEntity<byte[]> generateProject(@RequestBody ProjectRequest request) throws IOException {
        // Get the appropriate stack provider
        StackType stackType = request.getStackType() != null ? request.getStackType() : StackType.SPRING;
        StackProvider provider = stackProviderFactory.getProvider(stackType);
        
        // Generate the project
        byte[] zipContent = provider.generateProjectZip(request);
        
        // Determine filename
        String filename = getProjectName(request, stackType);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipContent);
    }
    
    /**
     * Generates project files and returns them as JSON for preview/editing in the IDE interface.
     * 
     * This endpoint creates all project files and returns them as a structured JSON response 
     * containing file paths, contents, and detected programming languages for syntax highlighting.
     * 
     * @param request The project configuration containing all generation parameters
     * @return ResponseEntity containing the list of generated files as FilePreview objects
     * @throws IOException If an error occurs during project generation
     */
    @PostMapping("/preview")
    public ResponseEntity<ProjectPreviewResponse> previewProject(@RequestBody ProjectRequest request) throws IOException {
        // Get the appropriate stack provider
        StackType stackType = request.getStackType() != null ? request.getStackType() : StackType.SPRING;
        StackProvider provider = stackProviderFactory.getProvider(stackType);
        
        // Generate preview files
        List<FilePreview> files = provider.generateProject(request);
        return ResponseEntity.ok(new ProjectPreviewResponse(files));
    }
    
    /**
     * Creates a ZIP file from a list of file previews (potentially edited by the user).
     * 
     * This endpoint allows users to download their edited files as a complete project.
     * It accepts a list of files with their paths and contents and returns them as a ZIP file.
     * 
     * @param request The download request containing files and artifact ID
     * @return ResponseEntity containing the ZIP file as byte array
     * @throws IOException If an error occurs during ZIP creation
     */
    @PostMapping("/from-files")
    public ResponseEntity<byte[]> generateProjectFromFiles(@RequestBody DownloadRequest request) throws IOException {
        byte[] zipContent = ZipUtils.createZipFromFilePreviews(request.getFiles(), request.getArtifactId());
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + request.getArtifactId() + ".zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipContent);
    }
    
    /**
     * Determines the project name for the ZIP filename.
     */
    private String getProjectName(ProjectRequest request, StackType stackType) {
        // Check artifactId (for Spring)
        if (request.getArtifactId() != null && !request.getArtifactId().isEmpty()) {
            return request.getArtifactId();
        }
        // Check name
        if (request.getName() != null && !request.getName().isEmpty()) {
            return request.getName().toLowerCase().replace(" ", "-");
        }
        // Default based on stack
        return stackType.getId() + "-project";
    }
}


