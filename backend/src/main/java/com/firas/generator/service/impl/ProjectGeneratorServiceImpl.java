package com.firas.generator.service.impl;

import com.firas.generator.model.DependencyMetadata;
import com.firas.generator.model.FilePreview;
import com.firas.generator.model.ProjectRequest;
import com.firas.generator.model.Table;
import com.firas.generator.service.DependencyRegistry;
import com.firas.generator.service.ProjectGeneratorService;
import com.firas.generator.service.TemplateService;
import com.firas.generator.util.SqlParser;
import com.firas.generator.util.ZipUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of ProjectGeneratorService for generating Spring Boot projects.
 * 
 * This service orchestrates the entire project generation process:
 * 1. Creates the standard Maven project structure (src/main/java, src/main/resources, etc.)
 * 2. Generates pom.xml with selected dependencies
 * 3. Generates the main Application class
 * 4. Generates application.properties configuration file
 * 5. Optionally generates CRUD code (entities, repositories, services, controllers) from SQL schemas
 * 6. Packages everything into a ZIP file for download
 * 
 * The service uses FreeMarker templates for code generation and creates projects in temporary
 * directories that are cleaned up after ZIP creation.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectGeneratorServiceImpl implements ProjectGeneratorService {

    /** Service for processing FreeMarker templates */
    private final TemplateService templateService;
    
    /** Utility for parsing SQL schemas */
    private final SqlParser sqlParser;
    
    /** Registry for resolving dependency metadata */
    private final DependencyRegistry dependencyRegistry;

    /**
     * Generates a complete Spring Boot project based on the provided request.
     * 
     * This method creates a temporary directory, generates all project files,
     * packages them into a ZIP file, and cleans up the temporary directory.
     * 
     * @param request The project configuration
     * @return Byte array containing the ZIP file
     * @throws IOException If an error occurs during generation
     */
    @Override
    public byte[] generateProject(ProjectRequest request) throws IOException {
        Path tempDir = Files.createTempDirectory("project-gen-");
        File rootDir = tempDir.toFile();
        
        try {
            String baseDirName = request.getArtifactId();
            File projectDir = new File(rootDir, baseDirName);
            projectDir.mkdirs();

            // Generate structure
            generateStructure(projectDir, request);
            
            // Generate pom.xml
            generatePom(projectDir, request);
            
            // Generate Main Class
            generateMainClass(projectDir, request);
            
            // Generate Application Properties
            generateApplicationProperties(projectDir, request);
            
            // Generate CRUD from SQL
            if (request.getTables() != null && !request.getTables().isEmpty()) {
                generateCrud(projectDir, request);
            }

            // Zip the directory
            return ZipUtils.zipDirectory(projectDir);
            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtils.deleteDirectory(rootDir);
        }
    }

    /**
     * Creates the standard Maven project directory structure.
     * 
     * @param projectDir The root project directory
     * @param request The project configuration
     */
    private void generateStructure(File projectDir, ProjectRequest request) {
        String packagePath = request.getPackageName().replace(".", "/");
        File javaSrc = new File(projectDir, "src/main/java/" + packagePath);
        javaSrc.mkdirs();
        
        File resources = new File(projectDir, "src/main/resources");
        resources.mkdirs();
        
        File testSrc = new File(projectDir, "src/test/java/" + packagePath);
        testSrc.mkdirs();
    }

    /**
     * Generates the Maven pom.xml file with dependencies.
     * 
     * @param projectDir The root project directory
     * @param request The project configuration
     */
    private void generatePom(File projectDir, ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        System.out.println("dependencies before resolvation " + request.getDependencies());
        
        // Resolve dependencies from registry
        List<DependencyMetadata> resolvedDependencies = request.getDependencies();

        System.out.println("dependencies after resolvation " + resolvedDependencies);
        model.put("dependencies", resolvedDependencies);
        
        // Check if Lombok is in dependencies
        boolean hasLombok = resolvedDependencies.stream()
                .anyMatch(dep -> "lombok".equals(dep.getId()));
        model.put("hasLombok", hasLombok);
        
        templateService.generateFile("pom.xml.ftl", model, new File(projectDir, "pom.xml"));
    }

    /**
     * Generates the main Spring Boot application class.
     * 
     * @param projectDir The root project directory
     * @param request The project configuration
     */
    private void generateMainClass(File projectDir, ProjectRequest request) {
        String packagePath = request.getPackageName().replace(".", "/");
        File javaSrc = new File(projectDir, "src/main/java/" + packagePath);
        
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        
        String className = toClassName(request.getName()) + "Application";
        model.put("className", className);
        
        templateService.generateFile("Application.java.ftl", model, new File(javaSrc, className + ".java"));
    }
    
    /**
     * Generates the application.properties configuration file.
     * 
     * @param projectDir The root project directory
     * @param request The project configuration
     */
    private void generateApplicationProperties(File projectDir, ProjectRequest request) {
        File resources = new File(projectDir, "src/main/resources");
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        templateService.generateFile("application.properties.ftl", model, new File(resources, "application.properties"));
    }

    /**
     * Generates CRUD code (entities, repositories, services, controllers) from SQL schema.
     * 
     * Parses the SQL content from the request and generates JPA entities with their
     * corresponding repository, service, and controller classes.
     * 
     * @param projectDir The root project directory
     * @param request The project configuration containing SQL content
     */
    private void generateCrud(File projectDir, ProjectRequest request) throws SQLException {
        List<Table> tables = request.getTables();
        String packagePath = request.getPackageName().replace(".", "/");
        File javaSrc = new File(projectDir, "src/main/java/" + packagePath);
        
        for (Table table : tables) {
            Map<String, Object> model = new HashMap<>();
            model.put("table", table);
            model.put("packageName", request.getPackageName());
            
            // Entity
            createFile(javaSrc, "entity", table.getClassName() + ".java", "Entity.ftl", model);
            
            // Repository
            createFile(javaSrc, "repository", table.getClassName() + "Repository.java", "Repository.ftl", model);
            
            // Service
            createFile(javaSrc, "service", table.getClassName() + "Service.java", "Service.ftl", model);
            
            // Controller
            createFile(javaSrc, "controller", table.getClassName() + "Controller.java", "Controller.ftl", model);
        }
    }
    
    /**
     * Creates a file from a template in a specific package subdirectory.
     * 
     * @param javaSrc The base Java source directory
     * @param subPackage The sub-package name (e.g., "entity", "repository")
     * @param fileName The output file name
     * @param templateName The FreeMarker template name
     * @param model The data model for template processing
     */
    private void createFile(File javaSrc, String subPackage, String fileName, String templateName, Map<String, Object> model) {
        File packageDir = new File(javaSrc, subPackage);
        packageDir.mkdirs();
        templateService.generateFile(templateName, model, new File(packageDir, fileName));
    }

    /**
     * Converts a project name to a valid Java class name.
     * 
     * @param name The project name
     * @return Capitalized class name (defaults to "Demo" if name is empty)
     */
    private String toClassName(String name) {
        if (name == null || name.isEmpty()) return "Demo";
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    
    /**
     * Generates project files and returns them as a list for preview/editing.
     * Creates all project files in a temporary directory, reads them into FilePreview objects,
     * and then cleans up the temporary directory.
     * 
     * @param request The project configuration
     * @return List of FilePreview objects containing file paths and contents
     * @throws IOException If an error occurs during generation
     */
    @Override
    public List<FilePreview> generateProjectPreview(ProjectRequest request) throws IOException {
        Path tempDir = Files.createTempDirectory("project-preview-");
        File rootDir = tempDir.toFile();
        
        try {
            String baseDirName = request.getArtifactId();
            File projectDir = new File(rootDir, baseDirName);
            projectDir.mkdirs();

            // Generate structure
            generateStructure(projectDir, request);
            
            // Generate pom.xml
            generatePom(projectDir, request);
            
            // Generate Main Class
            generateMainClass(projectDir, request);
            
            // Generate Application Properties
            generateApplicationProperties(projectDir, request);
            
            // Generate CRUD from SQL
            if (request.getTables() != null && !request.getTables().isEmpty()) {
                generateCrud(projectDir, request);
            }

            // Read all generated files into FilePreview objects
            return readProjectFiles(projectDir, baseDirName);
            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtils.deleteDirectory(rootDir);
        }
    }
    
    /**
     * Reads all files from a project directory into FilePreview objects.
     * 
     * @param projectDir The project root directory
     * @param baseDirName The base directory name (used for relative paths)
     * @return List of FilePreview objects
     * @throws IOException If an error occurs reading files
     */
    private List<FilePreview> readProjectFiles(File projectDir, String baseDirName) throws IOException {
        List<FilePreview> files = new java.util.ArrayList<>();
        
        Files.walk(projectDir.toPath())
            .filter(Files::isRegularFile)
            .forEach(path -> {
                try {
                    String content = Files.readString(path);
                    String relativePath = projectDir.toPath().relativize(path).toString().replace("\\", "/");
                    String language = detectLanguage(relativePath);
                    
                    files.add(new FilePreview(relativePath, content, language));
                } catch (IOException e) {
                    log.error("Error reading file: " + path, e);
                }
            });
        
        return files;
    }
    
    /**
     * Detects programming language from file extension.
     * 
     * @param filePath The file path
     * @return Language identifier for syntax highlighting
     */
    private String detectLanguage(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".java")) return "java";
        if (lowerPath.endsWith(".xml")) return "xml";
        if (lowerPath.endsWith(".properties")) return "properties";
        if (lowerPath.endsWith(".yml") || lowerPath.endsWith(".yaml")) return "yaml";
        if (lowerPath.endsWith(".json")) return "json";
        if (lowerPath.endsWith(".md")) return "markdown";
        if (lowerPath.endsWith(".sql")) return "sql";
        return "text";
    }
    
    /**
     * Creates a ZIP file from a list of file previews.
     * This allows users to download their edited files as a complete project.
     * 
     * @param files List of files with paths and contents
     * @param artifactId Project artifact ID for naming the ZIP
     * @return Byte array containing the ZIP file
     * @throws IOException If an error occurs during ZIP creation
     */
    @Override
    public byte[] generateZipFromFiles(List<FilePreview> files, String artifactId) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Files list cannot be null or empty");
        }

        if (artifactId == null || artifactId.trim().isEmpty()) {
            throw new IllegalArgumentException("Artifact ID cannot be null or empty");
        }

        Path tempDir = Files.createTempDirectory("project-from-files-");
        File rootDir = tempDir.toFile();

        try {
            File projectDir = new File(rootDir, artifactId);
            if (!projectDir.mkdirs() && !projectDir.exists()) {
                throw new IOException("Failed to create project directory: " + projectDir.getAbsolutePath());
            }

            // Write all files to the temporary directory
            for (int i = 0; i < files.size(); i++) {
                FilePreview file = files.get(i);

                // Validate file
                if (file == null) {
                    throw new IllegalArgumentException("File at index " + i + " is null");
                }

                String filePath = file.getPath();
                String content = file.getContent();

                if (filePath == null || filePath.trim().isEmpty()) {
                    throw new IllegalArgumentException("File path at index " + i + " is null or empty");
                }

                if (content == null) {
                    // Set empty content instead of null
                    content = "";
                }

                // Clean up the path
                filePath = filePath.replace("\\", "/");
                if (filePath.startsWith("/")) {
                    filePath = filePath.substring(1);
                }

                File targetFile = new File(projectDir, filePath);

                // Create parent directories if they don't exist
                File parentDir = targetFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs() && !parentDir.exists()) {
                        throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
                    }
                }

                // Write file content
                try {
                    Files.writeString(targetFile.toPath(), content, StandardCharsets.UTF_8);
                    log.info("Written file: {}", targetFile.getAbsolutePath());
                } catch (Exception e) {
                    throw new IOException("Failed to write file: " + filePath, e);
                }
            }

            // Zip the directory
            return ZipUtils.zipDirectory(projectDir);

        } catch (Exception e) {
            log.error("Error generating zip from files", e);
            throw e;
        } finally {
            try {
                if (rootDir.exists()) {
                    FileUtils.deleteDirectory(rootDir);
                }
            } catch (IOException e) {
                log.warn("Failed to clean up temporary directory: {}", rootDir.getAbsolutePath(), e);
            }
        }
    }
}
