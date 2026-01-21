package com.firas.generator.stack.spring;

import com.firas.generator.model.DependencyMetadata;
import com.firas.generator.model.FilePreview;
import com.firas.generator.model.ProjectRequest;
import com.firas.generator.model.RelationshipType;
import com.firas.generator.model.Table;
import com.firas.generator.model.config.SpringConfig;
import com.firas.generator.service.TemplateService;
import com.firas.generator.stack.*;
import com.firas.generator.util.ZipUtils;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public class SpringStackProvider implements StackProvider {
    
    private static final String TEMPLATE_DIR = "spring/";
    
    private final TemplateService templateService;
    private final SpringCodeGenerator codeGenerator;
    private final SpringTypeMapper typeMapper;
    private final SpringDependencyProvider dependencyProvider;
    
    public SpringStackProvider(
            TemplateService templateService,
            SpringCodeGenerator codeGenerator,
            SpringTypeMapper typeMapper,
            SpringDependencyProvider dependencyProvider) {
        this.templateService = templateService;
        this.codeGenerator = codeGenerator;
        this.typeMapper = typeMapper;
        this.dependencyProvider = dependencyProvider;
    }
    
    @Override
    public StackType getStackType() {
        return StackType.SPRING;
    }
    
    @Override
    public String getTemplateDirectory() {
        return "spring";
    }
    
    @Override
    public TypeMapper getTypeMapper() {
        return typeMapper;
    }
    
    @Override
    public CodeGenerator getCodeGenerator() {
        return codeGenerator;
    }
    
    @Override
    public DependencyProvider getDependencyProvider() {
        return dependencyProvider;
    }
    
    @Override
    public List<FilePreview> generateProject(ProjectRequest request) throws IOException {
        // Apply type mappings to all columns
        applyTypeMappings(request);
        
        List<FilePreview> files = new ArrayList<>();
        
        // Generate project structure files
        files.add(generatePom(request));
        files.add(generateMainClass(request));
        files.add(generateApplicationProperties(request));

        // Handle security configuration specific table modifications
        if (request.getSecurityConfig() != null && request.getSecurityConfig().isEnabled() && request.getTables() != null) {
            com.firas.generator.model.config.SecurityConfig security = request.getSecurityConfig();
            request.getTables().stream()
                .filter(table -> table.getName().equalsIgnoreCase(security.getPrincipalEntity()))
                .findFirst()
                .ifPresent(table -> {
                    System.out.println("FOUND Principal Table: " + table.getName());
                    // 1. Inject Metadata for Entity.ftl
                    table.addMetadata("isUserDetails", true);
                    System.out.println("Injected isUserDetails=true for table " + table.getName());
                    table.addMetadata("usernameField", security.getUsernameField());
                    table.addMetadata("passwordField", security.getPasswordField());
                    table.addMetadata("roleStrategy", security.getRoleStrategy());
                    table.addMetadata("roleEntity", security.getRoleEntity());
                    table.addMetadata("rbacMode", security.getRbacMode());
                    table.addMetadata("roleField", "role"); // For Static mode: field name storing the Role enum

                    // 2. Ensure Password Column Exists
                    boolean hasPassword = table.getColumns().stream()
                            .anyMatch(c -> c.getFieldName().equals(security.getPasswordField()));
                    
                    System.out.println("Has Password Field '" + security.getPasswordField() + "'? " + hasPassword);

                    if (!hasPassword) {
                        try {
                            System.out.println("Injecting missing password field '" + security.getPasswordField() + "' into principal entity '" + table.getName() + "'");
                            com.firas.generator.model.Column passwordCol = new com.firas.generator.model.Column();
                            passwordCol.setName(security.getPasswordField()); // DB name
                            passwordCol.setFieldName(security.getPasswordField());
                            passwordCol.setJavaType("String");
                            passwordCol.setType("VARCHAR(255)");
                            passwordCol.setNullable(false);
                            table.addColumn(passwordCol);
                            System.out.println("Password field injected. Column count now: " + table.getColumns().size());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // 3. Handle Role Entity Strategy M:N Injection
                    if ("ENTITY".equalsIgnoreCase(security.getRoleStrategy())) {
                        String roleEntityName = security.getRoleEntity();
                        System.out.println("Checking M:N injection for role entity: " + roleEntityName);
                        
                        Table roleTable = request.getTables().stream()
                                .filter(t -> t.getName().equalsIgnoreCase(roleEntityName))
                                .findFirst()
                                .orElse(null);

                        if (roleTable != null) {
                            System.out.println("Found Role Table: " + roleTable.getName());
                            // Check for existing relationship
                            System.out.println("Current relationships for " + table.getName() + ":");
                            table.getRelationships().forEach(r -> System.out.println(" - Target: " + r.getTargetClassName()));

                            boolean hasRelation = table.getRelationships().stream()
                                    .anyMatch(r -> r.getTargetClassName().equalsIgnoreCase(roleTable.getClassName()));
                            
                            System.out.println("Has existing relation to Role? " + hasRelation);

                            if (!hasRelation) {
                                System.out.println("Injecting missing M:N relationship between '" + table.getName() + "' and '" + roleTable.getName() + "'");
                                // Inject logical relationship (Owner side on User)
                                com.firas.generator.model.Relationship userToRole = new com.firas.generator.model.Relationship();
                                userToRole.setType(RelationshipType.MANY_TO_MANY);
                                userToRole.setFieldName("roles"); // Standard name
                                userToRole.setTargetClassName(roleTable.getClassName());
                                userToRole.setSourceTable(table.getName());
                                userToRole.setTargetTable(roleTable.getName());
                                userToRole.setJoinTable(table.getName().toLowerCase() + "_" + roleTable.getName().toLowerCase());
                                userToRole.setSourceColumn(table.getName().toLowerCase() + "_id");
                                userToRole.setTargetColumn(roleTable.getName().toLowerCase() + "_id");
                                table.addRelationship(userToRole);
                                System.out.println("Relationship injected. New count: " + table.getRelationships().size());
                            }
                        } else {
                            System.out.println("WARN: Role entity '" + roleEntityName + "' not found. Downgrading to STRING strategy.");
                            table.addMetadata("roleStrategy", "STRING");
                        }
                    }

                    // 4. Handle Dynamic RBAC Mode M:N Injection
                    if ("DYNAMIC".equalsIgnoreCase(security.getRbacMode())) {
                        System.out.println("Setting up Dynamic RBAC mode for principal entity: " + table.getName());
                        table.addMetadata("roleEntity", "Role"); // Dynamic mode always uses generated Role entity
                        
                        // Check for existing relationship to Role (exact match for auto-generated entity)
                        boolean hasRelation = table.getRelationships().stream()
                                .anyMatch(r -> "Role".equals(r.getTargetClassName()));
                        
                        if (!hasRelation) {
                            System.out.println("Injecting M:N relationship to auto-generated Role entity");
                            com.firas.generator.model.Relationship userToRole = new com.firas.generator.model.Relationship();
                            userToRole.setType(RelationshipType.MANY_TO_MANY);
                            userToRole.setFieldName("roles");
                            userToRole.setTargetClassName("Role");
                            userToRole.setSourceTable(table.getName());
                            userToRole.setTargetTable("roles");
                            userToRole.setJoinTable(table.getName().toLowerCase() + "_roles");
                            userToRole.setSourceColumn(table.getName().toLowerCase() + "_id");
                            userToRole.setTargetColumn("role_id");
                            table.addRelationship(userToRole);
                            System.out.println("Dynamic Role relationship injected. New count: " + table.getRelationships().size());
                        }
                    }
                });
        }

        // Generate Security Config if enabled
        if (request.getSecurityConfig() != null && request.getSecurityConfig().isEnabled()) {
            files.addAll(generateExtendedSecurityFiles(request));
            files.add(generateSecurityConfig(request));
            
            // Set security config on code generator for @PreAuthorize annotations
            codeGenerator.setSecurityConfig(request.getSecurityConfig());
        }
        
        // Generate CRUD code if tables are provided
        if (request.getTables() != null && !request.getTables().isEmpty()) {
            for (Table table : request.getTables()) {
                if (table.isJoinTable()) {
                    continue; // Skip join tables
                }
                
                if (request.isIncludeEntity()) {
                    files.add(codeGenerator.generateEntity(table, request.getPackageName()));
                }
                if (request.isIncludeRepository()) {
                    files.add(codeGenerator.generateRepository(table, request.getPackageName()));
                }
                if (request.isIncludeService()) {
                    files.add(codeGenerator.generateService(table, request.getPackageName()));
                }
                if (request.isIncludeController()) {
                    files.add(codeGenerator.generateController(table, request.getPackageName()));
                }
                if (request.isIncludeDto()) {
                    files.add(codeGenerator.generateDto(table, request.getPackageName()));
                }
                if (request.isIncludeMapper()) {
                    files.add(codeGenerator.generateMapper(table, request.getPackageName()));
                }
                
                // Generate tests if enabled
                if (request.isIncludeTests()) {
                    if (request.isIncludeRepository()) {
                        files.add(codeGenerator.generateRepositoryTest(table, request.getPackageName()));
                    }
                    if (request.isIncludeController()) {
                        files.add(codeGenerator.generateControllerTest(table, request.getPackageName()));
                    }
                }
            }
        }
        
        // Generate Docker files if enabled
        if (request.isIncludeDocker()) {
            files.addAll(generateDockerFiles(request));
        }
        
        return files;
    }
    
    @Override
    public byte[] generateProjectZip(ProjectRequest request) throws IOException {
        List<FilePreview> files = generateProject(request);
        return createZipFromFiles(files, getProjectName(request));
    }
    
    // ==================== Spring-Specific Generation Methods ====================
    
    /**
     * Generates the Maven pom.xml file.
     */
    private FilePreview generatePom(ProjectRequest request) {
        SpringConfig config = request.getEffectiveSpringConfig();
        
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        model.put("springConfig", config);
        
        List<DependencyMetadata> dependencies = request.getDependencies();
        if (dependencies == null) {
            dependencies = new ArrayList<>();
        }

        model.put("dependencies", dependencies);
        
        // Check if Lombok is in dependencies
        boolean hasLombok = dependencies.stream()
                .anyMatch(dep -> "lombok".equals(dep.getId()));
        model.put("hasLombok", hasLombok);
        
        // Check if JWT authentication is enabled
        boolean hasJwt = request.getSecurityConfig() != null 
                && "JWT".equalsIgnoreCase(request.getSecurityConfig().getAuthenticationType());
        model.put("hasJwt", hasJwt);
        
        String content = templateService.processTemplateToString(TEMPLATE_DIR + "pom.xml.ftl", model);
        return new FilePreview("pom.xml", content, "xml");
    }
    
    /**
     * Generates the main Spring Boot application class.
     */
    private FilePreview generateMainClass(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        
        String className = toClassName(request.getName()) + "Application";
        model.put("className", className);
        
        String content = templateService.processTemplateToString(TEMPLATE_DIR + "Application.java.ftl", model);
        String packagePath = request.getPackageName().replace(".", "/");
        String path = "src/main/java/" + packagePath + "/" + className + ".java";
        
        return new FilePreview(path, content, "java");
    }
    
    /**
     * Generates the application.properties file.
     */
    private FilePreview generateApplicationProperties(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        
        // Check if JWT authentication is enabled
        boolean hasJwt = request.getSecurityConfig() != null 
                && "JWT".equalsIgnoreCase(request.getSecurityConfig().getAuthenticationType());
        model.put("hasJwt", hasJwt);
        
        String content = templateService.processTemplateToString(TEMPLATE_DIR + "application.properties.ftl", model);
        return new FilePreview("src/main/resources/application.properties", content, "properties");
    }

    /**
     * Generates the Security Configuration class.
     */
    private FilePreview generateSecurityConfig(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        model.put("packageName", request.getPackageName());
        model.put("security", request.getSecurityConfig());
        
        String content = templateService.processTemplateToString(TEMPLATE_DIR + "SecurityConfig.ftl", model);
        String path = "src/main/java/" + request.getPackageName().replace(".", "/") + "/config/SecurityConfig.java";
        
        return new FilePreview(path, content, "java");
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Converts a project name to a valid Java class name.
     */
    private String toClassName(String name) {
        if (name == null || name.isEmpty()) return "Demo";
        // Remove non-alphanumeric characters and capitalize
        String cleaned = name.replaceAll("[^a-zA-Z0-9]", "");
        if (cleaned.isEmpty()) return "Demo";
        return cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
    }
    
    /**
     * Gets the project name from the request.
     * Uses artifactId from SpringConfig or falls back to project name.
     */
    private String getProjectName(ProjectRequest request) {
        SpringConfig config = request.getEffectiveSpringConfig();
        
        if (config.getArtifactId() != null && !config.getArtifactId().isEmpty()) {
            return config.getArtifactId();
        }
        if (request.getName() != null && !request.getName().isEmpty()) {
            return request.getName().toLowerCase().replace(" ", "-");
        }
        return "spring-project";
    }
    
    /**
     * Creates a ZIP file from the list of file previews.
     */
    private byte[] createZipFromFiles(List<FilePreview> files, String projectName) throws IOException {
        Path tempDir = Files.createTempDirectory("spring-gen-");
        File projectDir = new File(tempDir.toFile(), projectName);
        projectDir.mkdirs();
        
        try {
            // Write all files to temp directory
            for (FilePreview file : files) {
                Path filePath = projectDir.toPath().resolve(file.getPath());
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, file.getContent(), StandardCharsets.UTF_8);
            }
            
            // Create ZIP
            return ZipUtils.zipDirectory(projectDir);
        } finally {
            // Cleanup temp directory
            try {
                FileUtils.deleteDirectory(tempDir.toFile());
            } catch (IOException e) {
                System.out.println("WARN: Failed to clean up temp directory: " + tempDir + " " + e.getMessage());
            }
        }

    }

    private List<FilePreview> generateExtendedSecurityFiles(ProjectRequest request) {
        List<FilePreview> files = new ArrayList<>();
        com.firas.generator.model.config.SecurityConfig security = request.getSecurityConfig();
        String basePath = "src/main/java/" + request.getPackageName().replace(".", "/") + "/";

        // 1. ApplicationConfig (Always)
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        model.put("packageName", request.getPackageName());
        String appConfigContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/ApplicationConfig.ftl", model);
        files.add(new FilePreview(basePath + "config/ApplicationConfig.java", appConfigContent, "java"));

        // 2. CustomUserDetailsService (If Principal set)
        if (security.getPrincipalEntity() != null) {
            Map<String, Object> tdsModel = new HashMap<>();
            tdsModel.put("packageName", request.getPackageName());
            tdsModel.put("repositoryName", security.getPrincipalEntity() + "Repository");
            tdsModel.put("usernameField", security.getUsernameField());
            String tdsContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/CustomUserDetailsService.ftl", tdsModel);
            files.add(new FilePreview(basePath + "service/auth/CustomUserDetailsService.java", tdsContent, "java"));
        }

        // 3. Static RBAC Mode: Generate Permission and Role Enums
        if ("STATIC".equalsIgnoreCase(security.getRbacMode())) {
            Map<String, Object> rbacModel = new HashMap<>();
            rbacModel.put("packageName", request.getPackageName());
            rbacModel.put("permissions", security.getPermissions());
            rbacModel.put("definedRoles", security.getDefinedRoles());
            
            // Generate Permission.java enum
            String permissionContent = templateService.processTemplateToString(TEMPLATE_DIR + "Permission.ftl", rbacModel);
            files.add(new FilePreview(basePath + "security/Permission.java", permissionContent, "java"));
            
            // Generate Role.java enum
            String roleContent = templateService.processTemplateToString(TEMPLATE_DIR + "Role.ftl", rbacModel);
            files.add(new FilePreview(basePath + "security/Role.java", roleContent, "java"));
        }

        // 4. Dynamic RBAC Mode: Generate Role JPA Entity
        if ("DYNAMIC".equalsIgnoreCase(security.getRbacMode())) {
            Map<String, Object> rbacModel = new HashMap<>();
            rbacModel.put("packageName", request.getPackageName());
            
            // Generate Role.java JPA entity (with @ElementCollection for permissions)
            String roleEntityContent = templateService.processTemplateToString(TEMPLATE_DIR + "RoleEntity.ftl", rbacModel);
            files.add(new FilePreview(basePath + "entity/Role.java", roleEntityContent, "java"));
            
            // Generate RoleRepository.java
            Map<String, Object> repoModel = new HashMap<>();
            repoModel.put("packageName", request.getPackageName());
            String roleRepoContent = generateRoleRepository(request.getPackageName());
            files.add(new FilePreview(basePath + "repository/RoleRepository.java", roleRepoContent, "java"));
        }

        // 5. JWT Components
        if ("JWT".equalsIgnoreCase(security.getAuthenticationType())) {
            Map<String, Object> jwtModel = new HashMap<>();
            jwtModel.put("request", request);
            jwtModel.put("packageName", request.getPackageName());
            jwtModel.put("security", security);
            
            // JwtUtil
            String jwtUtilContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/JwtUtil.ftl", jwtModel);
            files.add(new FilePreview(basePath + "config/JwtUtil.java", jwtUtilContent, "java"));

            // JwtFilter
            String jwtFilterContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/JwtFilter.ftl", jwtModel);
            files.add(new FilePreview(basePath + "config/JwtAuthenticationFilter.java", jwtFilterContent, "java"));
            
            // Auth DTOs
            String authRequestContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/AuthRequest.ftl", jwtModel);
            files.add(new FilePreview(basePath + "dto/AuthRequest.java", authRequestContent, "java"));
            
            String authResponseContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/AuthResponse.ftl", jwtModel);
            files.add(new FilePreview(basePath + "dto/AuthResponse.java", authResponseContent, "java"));
            
            String registerRequestContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/RegisterRequest.ftl", jwtModel);
            files.add(new FilePreview(basePath + "dto/RegisterRequest.java", registerRequestContent, "java"));
            
            // Auth Controller
            String authControllerContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/AuthController.ftl", jwtModel);
            files.add(new FilePreview(basePath + "controller/AuthController.java", authControllerContent, "java"));
        }

        return files;
    }

    /**
     * Generates Docker-related files (Dockerfile, docker-compose.yml, .dockerignore).
     */
    private List<FilePreview> generateDockerFiles(ProjectRequest request) {
        List<FilePreview> files = new ArrayList<>();
        
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        
        // Check for specific database dependencies
        List<DependencyMetadata> dependencies = request.getDependencies();
        boolean hasMysql = dependencies != null && dependencies.stream()
                .anyMatch(dep -> "mysql".equals(dep.getId()) || "mysql-connector-java".equals(dep.getArtifactId()));
        boolean hasPostgres = dependencies != null && dependencies.stream()
                .anyMatch(dep -> "postgresql".equals(dep.getId()) || "postgresql".equals(dep.getArtifactId()));
        boolean hasMariadb = dependencies != null && dependencies.stream()
                .anyMatch(dep -> "mariadb".equals(dep.getId()) || "mariadb-java-client".equals(dep.getArtifactId()));
        boolean hasRedis = dependencies != null && dependencies.stream()
                .anyMatch(dep -> "data-redis".equals(dep.getId()) || dep.getArtifactId() != null && dep.getArtifactId().contains("redis"));
        boolean hasMongodb = dependencies != null && dependencies.stream()
                .anyMatch(dep -> "data-mongodb".equals(dep.getId()) || dep.getArtifactId() != null && dep.getArtifactId().contains("mongo"));
        
        model.put("hasMysql", hasMysql);
        model.put("hasPostgres", hasPostgres);
        model.put("hasMariadb", hasMariadb);
        model.put("hasRedis", hasRedis);
        model.put("hasMongodb", hasMongodb);
        
        // Check for JWT
        boolean hasJwt = request.getSecurityConfig() != null 
                && "JWT".equalsIgnoreCase(request.getSecurityConfig().getAuthenticationType());
        model.put("hasJwt", hasJwt);
        
        // Dockerfile
        String dockerfileContent = templateService.processTemplateToString(TEMPLATE_DIR + "Dockerfile.ftl", model);
        files.add(new FilePreview("Dockerfile", dockerfileContent, "dockerfile"));
        
        // docker-compose.yml
        String dockerComposeContent = templateService.processTemplateToString(TEMPLATE_DIR + "docker-compose.yml.ftl", model);
        files.add(new FilePreview("docker-compose.yml", dockerComposeContent, "yaml"));
        
        // .dockerignore
        String dockerignoreContent = templateService.processTemplateToString(TEMPLATE_DIR + ".dockerignore.ftl", model);
        files.add(new FilePreview(".dockerignore", dockerignoreContent, "text"));
        
        return files;
    }

    /**
     * Generates a simple RoleRepository for Dynamic RBAC mode.
     */
    private String generateRoleRepository(String packageName) {
        return "package " + packageName + ".repository;\n\n" +
               "import " + packageName + ".entity.Role;\n" +
               "import org.springframework.data.jpa.repository.JpaRepository;\n" +
               "import org.springframework.stereotype.Repository;\n\n" +
               "import java.util.Optional;\n\n" +
               "@Repository\n" +
               "public interface RoleRepository extends JpaRepository<Role, Long> {\n" +
               "    Optional<Role> findByName(String name);\n" +
               "}\n";
    }
}
