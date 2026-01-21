package com.firas.generator.stack;

import com.firas.generator.model.FilePreview;
import com.firas.generator.model.ProjectRequest;
import com.firas.generator.service.TemplateService;
import com.firas.generator.util.ZipUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Abstract base class for StackProvider implementations.
 * 
 * Implements the Template Method pattern: defines the skeleton of the
 * project generation algorithm, deferring stack-specific steps to subclasses.
 * 
 * Common functionality includes:
 * - Project structure creation workflow
 * - ZIP file generation
 * - Type mapping application
 * - File preview reading
 * 
 * Subclasses must implement:
 * - createProjectStructure(): Generate project directories and base files
 * - generateBuildConfig(): Generate build configuration (pom.xml, package.json, etc.)
 * - generateMainFile(): Generate main entry point
 * - generateConfigFiles(): Generate configuration files
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public abstract class AbstractStackProvider implements StackProvider {
    
    protected final TemplateService templateService;
    protected final ZipUtils zipUtils;
    
    protected AbstractStackProvider(TemplateService templateService, ZipUtils zipUtils) {
        this.templateService = templateService;
        this.zipUtils = zipUtils;
    }
    
    // ==================== Template Method ====================
    
    /**
     * Template Method: Defines the project generation workflow.
     * Subclasses implement abstract methods to customize each step.
     */
    @Override
    public List<FilePreview> generateProject(ProjectRequest request) throws IOException {
        // Apply type mappings to all columns
        applyTypeMappings(request);
        
        List<FilePreview> files = new ArrayList<>();
        
        // Step 1: Create project structure and base files
        files.addAll(createProjectStructure(request));
        
        // Step 2: Generate build configuration
        FilePreview buildConfig = generateBuildConfig(request);
        if (buildConfig != null) {
            files.add(buildConfig);
        }
        
        // Step 3: Generate main entry point
        FilePreview mainFile = generateMainFile(request);
        if (mainFile != null) {
            files.add(mainFile);
        }
        
        // Step 4: Generate configuration files
        files.addAll(generateConfigFiles(request));
        
        // Step 5: Generate CRUD code if tables are provided
        if (hasTables(request)) {
            files.addAll(getCodeGenerator().generateAllCrud(request));
        }
        
        return files;
    }
    
    /**
     * Generates project as a ZIP file.
     * Uses generateProject() and packages the results.
     */
    @Override
    public byte[] generateProjectZip(ProjectRequest request) throws IOException {
        List<FilePreview> files = generateProject(request);
        return createZipFromFiles(files, getProjectName(request));
    }
    
    // ==================== Abstract Methods ====================
    
    /**
     * Creates the project directory structure and any base files.
     * For Spring: src/main/java, src/main/resources, etc.
     * For Node: src/, public/, etc.
     */
    protected abstract List<FilePreview> createProjectStructure(ProjectRequest request) throws IOException;
    
    /**
     * Generates the build configuration file.
     * For Spring: pom.xml
     * For Node/Nest: package.json
     * For FastAPI: requirements.txt + pyproject.toml
     */
    protected abstract FilePreview generateBuildConfig(ProjectRequest request) throws IOException;
    
    /**
     * Generates the main entry point file.
     * For Spring: Application.java
     * For Node: index.js or app.js
     * For Nest: main.ts
     * For FastAPI: main.py
     */
    protected abstract FilePreview generateMainFile(ProjectRequest request) throws IOException;
    
    /**
     * Generates configuration files.
     * For Spring: application.properties
     * For Node: .env, config.js
     * For Nest: .env, nest-cli.json
     * For FastAPI: .env, config.py
     */
    protected abstract List<FilePreview> generateConfigFiles(ProjectRequest request) throws IOException;
    
    // ==================== Helper Methods ====================
    
    /**
     * Creates a ZIP file from the list of file previews.
     */
    protected byte[] createZipFromFiles(List<FilePreview> files, String projectName) throws IOException {
        Path tempDir = Files.createTempDirectory("gen_" + projectName);
        Path projectDir = tempDir.resolve(projectName);
        Files.createDirectories(projectDir);
        
        try {
            // Write all files to temp directory
            for (FilePreview file : files) {
                Path filePath = projectDir.resolve(file.getPath());
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, file.getContent(), StandardCharsets.UTF_8);
            }
            
            // Create ZIP
            return zipUtils.zipDirectory(projectDir.toFile());
        } finally {
            // Cleanup temp directory
            deleteDirectory(tempDir);
        }
    }
    
    /**
     * Recursively deletes a directory.
     */
    protected void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }
    
    /**
     * Gets the project name from the request.
     */
    protected String getProjectName(ProjectRequest request) {
        if (request.getArtifactId() != null && !request.getArtifactId().isEmpty()) {
            return request.getArtifactId();
        }
        if (request.getName() != null && !request.getName().isEmpty()) {
            return request.getName().toLowerCase().replace(" ", "-");
        }
        return "generated-project";
    }
    
    /**
     * Checks if the request has tables for CRUD generation.
     */
    protected boolean hasTables(ProjectRequest request) {
        return request.getTables() != null && !request.getTables().isEmpty();
    }
    
    /**
     * Creates a FilePreview object.
     */
    protected FilePreview createFilePreview(String path, String content, String language) {
        FilePreview preview = new FilePreview();
        preview.setPath(path);
        preview.setContent(content);
        preview.setLanguage(language);
        return preview;
    }
    
    /**
     * Generates a file using a template.
     * 
     * @param templateName Template name (without stack prefix)
     * @param model Template data model
     * @return Generated content as string
     */
    protected String generateFromTemplate(String templateName, Map<String, Object> model) throws IOException {
        StringWriter writer = new StringWriter();
        templateService.processTemplate(getTemplateDirectory() + "/" + templateName, model, writer);
        return writer.toString();
    }
}
