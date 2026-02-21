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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public class SpringStackProvider implements StackProvider {

    private static final Logger log = LoggerFactory.getLogger(SpringStackProvider.class);
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

        // Set Spring config on code generator for project structure support
        codeGenerator.setSpringConfig(request.getEffectiveSpringConfig());

        try {
        
        // Generate project structure files
        SpringConfig springConfig = request.getEffectiveSpringConfig();
        if ("gradle".equalsIgnoreCase(springConfig.getBuildTool())) {
            files.add(generateGradleBuild(request));
            files.add(generateSettingsGradle(request));
        } else {
            files.add(generatePom(request));
        }
        files.add(generateMainClass(request));
        if ("yml".equalsIgnoreCase(springConfig.getConfigFormat())) {
            files.add(generateApplicationYml(request));
            files.add(generateApplicationDevYml(request));
        } else {
            files.add(generateApplicationProperties(request));
            files.add(generateApplicationDevProperties(request));
        }
        files.add(generateGitignore());

        // Handle security configuration specific table modifications
        if (request.getSecurityConfig() != null && request.getSecurityConfig().isEnabled() && request.getTables() != null) {
            com.firas.generator.model.config.SecurityConfig security = request.getSecurityConfig();
            request.getTables().stream()
                .filter(table -> table.getName().equalsIgnoreCase(security.getPrincipalEntity()))
                .findFirst()
                .ifPresent(table -> {
                    log.debug("Found principal table: {}", table.getName());
                    // 1. Inject Metadata for Entity.ftl
                    table.addMetadata("isUserDetails", true);
                    table.addMetadata("usernameField", security.getUsernameField());
                    table.addMetadata("passwordField", security.getPasswordField());
                    table.addMetadata("roleStrategy", security.getRoleStrategy());
                    table.addMetadata("roleEntity", security.getRoleEntity());
                    table.addMetadata("rbacMode", security.getRbacMode());
                    table.addMetadata("roleField", "role"); // For Static mode: field name storing the Role enum

                    // 2. Ensure Password Column Exists
                    boolean hasPassword = table.getColumns().stream()
                            .anyMatch(c -> c.getFieldName().equals(security.getPasswordField()));

                    if (!hasPassword) {
                        try {
                            log.debug("Injecting missing password field '{}' into principal entity '{}'", security.getPasswordField(), table.getName());
                            com.firas.generator.model.Column passwordCol = new com.firas.generator.model.Column();
                            passwordCol.setName(security.getPasswordField()); // DB name
                            passwordCol.setFieldName(security.getPasswordField());
                            passwordCol.setJavaType("String");
                            passwordCol.setType("VARCHAR(255)");
                            passwordCol.setNullable(false);
                            table.addColumn(passwordCol);
                        } catch (Exception e) {
                            log.error("Failed to inject password field", e);
                        }
                    }

                    // 2.5 Ensure Password Reset Columns Exist
                    if (security.isPasswordResetEnabled()) {
                        String tokenField = security.getPasswordResetTokenField() != null ? security.getPasswordResetTokenField() : "resetToken";
                        String expiryField = security.getPasswordResetExpiryField() != null ? security.getPasswordResetExpiryField() : "resetTokenExpiry";
                        
                        boolean hasToken = table.getColumns().stream()
                                .anyMatch(c -> c.getFieldName().equals(tokenField));
                        if (!hasToken) {
                            com.firas.generator.model.Column tokenCol = new com.firas.generator.model.Column();
                            tokenCol.setName(tokenField.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase());
                            tokenCol.setFieldName(tokenField);
                            tokenCol.setJavaType("String");
                            tokenCol.setType("VARCHAR(255)");
                            tokenCol.setNullable(true);
                            table.addColumn(tokenCol);
                        }
                        
                        boolean hasExpiry = table.getColumns().stream()
                                .anyMatch(c -> c.getFieldName().equals(expiryField));
                        if (!hasExpiry) {
                            com.firas.generator.model.Column expiryCol = new com.firas.generator.model.Column();
                            expiryCol.setName(expiryField.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase());
                            expiryCol.setFieldName(expiryField);
                            // We use LocalDateTime so it matches what PasswordResetService expects
                            expiryCol.setJavaType("LocalDateTime");
                            expiryCol.setType("TIMESTAMP");
                            expiryCol.setNullable(true);
                            table.addColumn(expiryCol);
                        }
                    }

                    // 3. Handle Role Entity Strategy M:N Injection
                    if ("ENTITY".equalsIgnoreCase(security.getRoleStrategy())) {
                        String roleEntityName = security.getRoleEntity();

                        Table roleTable = request.getTables().stream()
                                .filter(t -> t.getName().equalsIgnoreCase(roleEntityName))
                                .findFirst()
                                .orElse(null);

                        if (roleTable != null) {
                            boolean hasRelation = table.getRelationships().stream()
                                    .anyMatch(r -> r.getTargetClassName().equalsIgnoreCase(roleTable.getClassName()));

                            if (!hasRelation) {
                                log.debug("Injecting M:N relationship between '{}' and '{}'", table.getName(), roleTable.getName());
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
                            }
                        } else {
                            log.warn("Role entity '{}' not found. Downgrading to STRING strategy.", roleEntityName);
                            table.addMetadata("roleStrategy", "STRING");
                        }
                    }

                    // 4. Handle Dynamic RBAC Mode M:N Injection
                    if ("DYNAMIC".equalsIgnoreCase(security.getRbacMode())) {
                        log.debug("Setting up Dynamic RBAC mode for principal entity: {}", table.getName());
                        table.addMetadata("roleEntity", "Role"); // Dynamic mode always uses generated Role entity

                        // Check for existing relationship to Role (exact match for auto-generated entity)
                        boolean hasRelation = table.getRelationships().stream()
                                .anyMatch(r -> "Role".equals(r.getTargetClassName()));

                        if (!hasRelation) {
                            log.debug("Injecting M:N relationship to auto-generated Role entity");
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

        // Generate CORS configuration
        files.add(generateCorsConfig(request));

        // Generate Global Exception Handler
        files.add(generateGlobalExceptionHandler(request));

        // Generate OpenAPI/Swagger configuration
        files.add(generateOpenApiConfig(request));

        // Generate E2E HTTP test file
        files.add(generateE2EHttp(request));

        // Generate CRUD code if tables are provided
        if (request.getTables() != null && !request.getTables().isEmpty()) {
            boolean includeDto = request.isIncludeDto() || request.isIncludeController() || request.isIncludeService();
            boolean includeMapper = request.isIncludeMapper() || includeDto;

            // Check for MapStruct in dependencies
            List<DependencyMetadata> deps = request.getDependencies();
            boolean hasMapStruct = deps != null && deps.stream()
                    .anyMatch(dep -> "mapstruct".equals(dep.getId())
                            || (dep.getArtifactId() != null && dep.getArtifactId().contains("mapstruct")));

            // Check for Rest-Assured in dependencies
            boolean hasRestAssured = deps != null && deps.stream()
                    .anyMatch(dep -> "rest-assured".equals(dep.getId())
                            || (dep.getArtifactId() != null && dep.getArtifactId().contains("rest-assured")));

            // Check for Testcontainers in dependencies
            boolean hasTestcontainers = deps != null && deps.stream()
                    .anyMatch(dep -> "testcontainers".equals(dep.getId())
                            || (dep.getArtifactId() != null && dep.getArtifactId().contains("testcontainers")));

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
                if (includeDto) {
                    files.add(codeGenerator.generateDto(table, request.getPackageName()));
                }
                if (includeMapper) {
                    if (hasMapStruct) {
                        files.add(codeGenerator.generateMapStructMapper(table, request.getPackageName()));
                    } else {
                        files.add(codeGenerator.generateMapper(table, request.getPackageName()));
                    }
                }
                
                // Generate tests if enabled
                if (request.isIncludeTests()) {
                    if (request.isIncludeRepository()) {
                        files.add(codeGenerator.generateRepositoryTest(table, request.getPackageName()));
                    }
                    if (request.isIncludeController()) {
                        if (hasRestAssured) {
                            files.add(codeGenerator.generateRestAssuredTest(table, request.getPackageName()));
                        } else {
                            files.add(codeGenerator.generateControllerTest(table, request.getPackageName()));
                        }
                    }
                }
            }

            // Generate Testcontainers config if enabled
            if (request.isIncludeTests() && hasTestcontainers) {
                files.addAll(generateTestcontainersFiles(request));
            }
        }
        
        // Generate Docker files if enabled
        if (request.isIncludeDocker()) {
            files.addAll(generateDockerFiles(request));
        }

        // Generate migration files if enabled
        SpringConfig config = request.getEffectiveSpringConfig();
        if (config.getMigrationTool() != null && !"none".equalsIgnoreCase(config.getMigrationTool())) {
            files.addAll(generateMigrationFiles(request, config));
        }

        return files;
        } finally {
            codeGenerator.clearConfig();
        }
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

        // Normalize dependency metadata before template rendering
        for (DependencyMetadata dep : dependencies) {
            if (dep.getVersion() != null && dep.getVersion().isBlank()) {
                dep.setVersion(null);
            }
            if (dep.getArtifactId() != null && dep.getArtifactId().equals("spring-boot-starter-webmvc")) {
                dep.setArtifactId("spring-boot-starter-web");
                if (dep.getGroupId() == null || dep.getGroupId().isBlank()) {
                    dep.setGroupId("org.springframework.boot");
                }
                if (dep.getId() == null || dep.getId().isBlank()) {
                    dep.setId("web");
                }
            }
        }

        // Auto-include essential dependencies based on project features
        boolean hasTables = request.getTables() != null && !request.getTables().isEmpty();
        boolean hasWeb = dependencies.stream().anyMatch(dep -> "web".equals(dep.getId())
                || "spring-boot-starter-web".equals(dep.getArtifactId()));
        boolean hasJpa = dependencies.stream().anyMatch(dep -> "data-jpa".equals(dep.getId())
                || "spring-boot-starter-data-jpa".equals(dep.getArtifactId()));
        boolean hasSecurity = dependencies.stream().anyMatch(dep -> "security".equals(dep.getId())
                || "spring-boot-starter-security".equals(dep.getArtifactId()));
        boolean hasValidation = dependencies.stream().anyMatch(dep -> "validation".equals(dep.getId())
                || "spring-boot-starter-validation".equals(dep.getArtifactId()));

        if (request.isIncludeController() && hasTables && !hasWeb) {
            DependencyMetadata webDep = new DependencyMetadata();
            webDep.setId("web");
            webDep.setGroupId("org.springframework.boot");
            webDep.setArtifactId("spring-boot-starter-web");
            dependencies.add(webDep);
        }
        if (hasTables && !hasJpa) {
            DependencyMetadata jpaDep = new DependencyMetadata();
            jpaDep.setId("data-jpa");
            jpaDep.setGroupId("org.springframework.boot");
            jpaDep.setArtifactId("spring-boot-starter-data-jpa");
            dependencies.add(jpaDep);
        }
        if (request.getSecurityConfig() != null && request.getSecurityConfig().isEnabled() && !hasSecurity) {
            DependencyMetadata secDep = new DependencyMetadata();
            secDep.setId("security");
            secDep.setGroupId("org.springframework.boot");
            secDep.setArtifactId("spring-boot-starter-security");
            dependencies.add(secDep);
        }
        if (hasTables && !hasValidation) {
            DependencyMetadata valDep = new DependencyMetadata();
            valDep.setId("validation");
            valDep.setGroupId("org.springframework.boot");
            valDep.setArtifactId("spring-boot-starter-validation");
            dependencies.add(valDep);
        }

        // Auto-include database driver based on databaseType
        if (hasTables && request.getDatabaseType() != null) {
            boolean hasDriver = dependencies.stream().anyMatch(dep -> {
                String aid = dep.getArtifactId();
                return aid != null && (aid.contains("mysql") || aid.contains("postgresql")
                        || aid.contains("mariadb") || aid.contains("h2")
                        || aid.contains("sqlserver") || aid.contains("sqlite"));
            });
            if (!hasDriver) {
                DependencyMetadata driverDep = new DependencyMetadata();
                driverDep.setScope("runtime");
                switch (request.getDatabaseType().toLowerCase()) {
                    case "mysql" -> {
                        driverDep.setId("mysql");
                        driverDep.setGroupId("com.mysql");
                        driverDep.setArtifactId("mysql-connector-j");
                    }
                    case "postgresql" -> {
                        driverDep.setId("postgresql");
                        driverDep.setGroupId("org.postgresql");
                        driverDep.setArtifactId("postgresql");
                    }
                    case "mariadb" -> {
                        driverDep.setId("mariadb");
                        driverDep.setGroupId("org.mariadb.jdbc");
                        driverDep.setArtifactId("mariadb-java-client");
                    }
                    case "h2" -> {
                        driverDep.setId("h2");
                        driverDep.setGroupId("com.h2database");
                        driverDep.setArtifactId("h2");
                    }
                    case "sqlserver" -> {
                        driverDep.setId("sqlserver");
                        driverDep.setGroupId("com.microsoft.sqlserver");
                        driverDep.setArtifactId("mssql-jdbc");
                    }
                    default -> driverDep = null;
                }
                if (driverDep != null) {
                    dependencies.add(driverDep);
                }
            }
        }

        // Auto-include H2 for dev profile (test scope) if not already present
        if (hasTables) {
            boolean hasH2 = dependencies.stream().anyMatch(dep -> "h2".equals(dep.getId())
                    || (dep.getArtifactId() != null && dep.getArtifactId().contains("h2")));
            if (!hasH2) {
                DependencyMetadata h2Dep = new DependencyMetadata();
                h2Dep.setId("h2");
                h2Dep.setGroupId("com.h2database");
                h2Dep.setArtifactId("h2");
                h2Dep.setScope("runtime");
                dependencies.add(h2Dep);
            }
        }

        // Auto-include springdoc-openapi for API documentation
        boolean hasSpringdoc = dependencies.stream().anyMatch(dep ->
                dep.getArtifactId() != null && dep.getArtifactId().contains("springdoc"));
        if (!hasSpringdoc) {
            DependencyMetadata springdocDep = new DependencyMetadata();
            springdocDep.setId("springdoc-openapi");
            springdocDep.setGroupId("org.springdoc");
            springdocDep.setArtifactId("springdoc-openapi-starter-webmvc-ui");
            // Choose compatible springdoc version based on Boot version
            // springdoc 2.8.x requires Spring Boot 3.4+ (Spring Framework 6.2+)
            // springdoc 2.3.0 works with Spring Boot 3.1-3.3
            String bootVer = request.getBootVersion();
            String springdocVersion = "2.3.0"; // Safe default for Boot 3.2.x
            if (bootVer != null) {
                try {
                    String[] parts = bootVer.split("\\.");
                    int minor = Integer.parseInt(parts[1]);
                    if (minor >= 4) {
                        springdocVersion = "2.8.4"; // Boot 3.4+
                    }
                } catch (Exception ignored) {}
            }
            springdocDep.setVersion(springdocVersion);
            dependencies.add(springdocDep);
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

        // Check for social logins
        boolean hasSocialLogins = request.getSecurityConfig() != null
                && request.getSecurityConfig().getSocialLogins() != null
                && !request.getSecurityConfig().getSocialLogins().isEmpty();
        model.put("hasSocialLogins", hasSocialLogins);

        // Check for Keycloak
        boolean hasKeycloak = request.getSecurityConfig() != null
                && (request.getSecurityConfig().isKeycloakEnabled()
                    || "KEYCLOAK_RS".equalsIgnoreCase(request.getSecurityConfig().getAuthenticationType())
                    || "KEYCLOAK_OAUTH".equalsIgnoreCase(request.getSecurityConfig().getAuthenticationType()));
        model.put("hasKeycloak", hasKeycloak);

        // Check for password reset
        boolean hasPasswordReset = request.getSecurityConfig() != null
                && request.getSecurityConfig().isPasswordResetEnabled();
        model.put("hasPasswordReset", hasPasswordReset);

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

        SpringConfig config = request.getEffectiveSpringConfig();
        boolean isKotlin = "kotlin".equalsIgnoreCase(config.getLanguage());

        if (isKotlin) {
            String content = templateService.processTemplateToString(TEMPLATE_DIR + "kotlin/Application.kt.ftl", model);
            String packagePath = request.getPackageName().replace(".", "/");
            String path = "src/main/kotlin/" + packagePath + "/" + className + ".kt";
            return new FilePreview(path, content, "kotlin");
        } else {
            String content = templateService.processTemplateToString(TEMPLATE_DIR + "Application.java.ftl", model);
            String packagePath = request.getPackageName().replace(".", "/");
            String path = "src/main/java/" + packagePath + "/" + className + ".java";
            return new FilePreview(path, content, "java");
        }
    }
    
    /**
     * Generates the application.properties file.
     */
    private FilePreview generateApplicationProperties(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        model.put("springConfig", request.getEffectiveSpringConfig());

        // Check if JWT authentication is enabled
        boolean hasJwt = request.getSecurityConfig() != null
                && "JWT".equalsIgnoreCase(request.getSecurityConfig().getAuthenticationType());
        model.put("hasJwt", hasJwt);
        
        String content = templateService.processTemplateToString(TEMPLATE_DIR + "application.properties.ftl", model);
        return new FilePreview("src/main/resources/application.properties", content, "properties");
    }

    /**
     * Generates the application-dev.properties file with H2 in-memory database for development.
     */
    private FilePreview generateApplicationDevProperties(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        model.put("springConfig", request.getEffectiveSpringConfig());

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "application-dev.properties.ftl", model);
        return new FilePreview("src/main/resources/application-dev.properties", content, "properties");
    }

    /**
     * Generates the Gradle build.gradle file.
     */
    private FilePreview generateGradleBuild(ProjectRequest request) {
        SpringConfig config = request.getEffectiveSpringConfig();

        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        model.put("springConfig", config);

        List<DependencyMetadata> dependencies = request.getDependencies();
        if (dependencies == null) {
            dependencies = new ArrayList<>();
        }
        model.put("dependencies", dependencies);

        boolean hasLombok = dependencies.stream()
                .anyMatch(dep -> "lombok".equals(dep.getId()));
        model.put("hasLombok", hasLombok);

        boolean hasJwt = request.getSecurityConfig() != null
                && "JWT".equalsIgnoreCase(request.getSecurityConfig().getAuthenticationType());
        model.put("hasJwt", hasJwt);

        boolean hasSocialLogins = request.getSecurityConfig() != null
                && request.getSecurityConfig().getSocialLogins() != null
                && !request.getSecurityConfig().getSocialLogins().isEmpty();
        model.put("hasSocialLogins", hasSocialLogins);

        boolean hasKeycloak = request.getSecurityConfig() != null
                && (request.getSecurityConfig().isKeycloakEnabled()
                    || "KEYCLOAK_RS".equalsIgnoreCase(request.getSecurityConfig().getAuthenticationType())
                    || "KEYCLOAK_OAUTH".equalsIgnoreCase(request.getSecurityConfig().getAuthenticationType()));
        model.put("hasKeycloak", hasKeycloak);

        boolean hasPasswordReset = request.getSecurityConfig() != null
                && request.getSecurityConfig().isPasswordResetEnabled();
        model.put("hasPasswordReset", hasPasswordReset);

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "build.gradle.ftl", model);
        return new FilePreview("build.gradle", content, "gradle");
    }

    /**
     * Generates the Gradle settings.gradle file.
     */
    private FilePreview generateSettingsGradle(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        model.put("springConfig", request.getEffectiveSpringConfig());

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "settings.gradle.ftl", model);
        return new FilePreview("settings.gradle", content, "gradle");
    }

    /**
     * Generates the application.yml file.
     */
    private FilePreview generateApplicationYml(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        model.put("springConfig", request.getEffectiveSpringConfig());

        boolean hasJwt = request.getSecurityConfig() != null
                && "JWT".equalsIgnoreCase(request.getSecurityConfig().getAuthenticationType());
        model.put("hasJwt", hasJwt);

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "application.yml.ftl", model);
        return new FilePreview("src/main/resources/application.yml", content, "yaml");
    }

    /**
     * Generates the application-dev.yml file with H2 in-memory database for development.
     */
    private FilePreview generateApplicationDevYml(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);
        model.put("springConfig", request.getEffectiveSpringConfig());

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "application-dev.yml.ftl", model);
        return new FilePreview("src/main/resources/application-dev.yml", content, "yaml");
    }

    /**
     * Generates the .gitignore file.
     */
    private FilePreview generateGitignore() {
        String content = templateService.processTemplateToString(TEMPLATE_DIR + ".gitignore.ftl", new HashMap<>());
        return new FilePreview(".gitignore", content, "text");
    }

    /**
     * Generates the GlobalExceptionHandler class.
     */
    private FilePreview generateGlobalExceptionHandler(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("packageName", request.getPackageName());
        boolean securityEnabled = request.getSecurityConfig() != null && request.getSecurityConfig().isEnabled();
        model.put("securityEnabled", securityEnabled);

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "GlobalExceptionHandler.ftl", model);
        String packagePath = request.getPackageName().replace(".", "/");
        String path = "src/main/java/" + packagePath + "/config/GlobalExceptionHandler.java";
        return new FilePreview(path, content, "java");
    }

    /**
     * Generates the OpenAPI/Swagger configuration class.
     */
    private FilePreview generateOpenApiConfig(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("packageName", request.getPackageName());
        model.put("request", request);

        // Check if JWT authentication is enabled for Swagger JWT integration
        boolean hasJwt = request.getSecurityConfig() != null
                && "JWT".equalsIgnoreCase(request.getSecurityConfig().getAuthenticationType());
        model.put("hasJwt", hasJwt);

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "OpenApiConfig.ftl", model);
        String packagePath = request.getPackageName().replace(".", "/");
        String path = "src/main/java/" + packagePath + "/config/OpenApiConfig.java";
        return new FilePreview(path, content, "java");
    }

    /**
     * Generates the CORS configuration class.
     */
    private FilePreview generateCorsConfig(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("packageName", request.getPackageName());

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "CorsConfig.ftl", model);
        String packagePath = request.getPackageName().replace(".", "/");
        String path = "src/main/java/" + packagePath + "/config/CorsConfig.java";
        return new FilePreview(path, content, "java");
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
                log.warn("Failed to clean up temp directory: {}", tempDir, e);
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

        // 2. CustomUserDetailsService (If Principal set and not using static fallback)
        boolean useStaticFallback = security.isStaticUserFallback() && security.getPrincipalEntity() == null;
        if (security.getPrincipalEntity() != null) {
            Map<String, Object> tdsModel = new HashMap<>();
            tdsModel.put("packageName", request.getPackageName());
            tdsModel.put("repositoryName", security.getPrincipalEntity() + "Repository");
            tdsModel.put("usernameField", security.getUsernameField());
            String tdsContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/CustomUserDetailsService.ftl", tdsModel);
            files.add(new FilePreview(basePath + "service/auth/CustomUserDetailsService.java", tdsContent, "java"));
        } else if (useStaticFallback) {
            // Generate InMemoryUserConfig for static user fallback
            Map<String, Object> fallbackModel = new HashMap<>();
            fallbackModel.put("packageName", request.getPackageName());
            fallbackModel.put("definedRoles", security.getDefinedRoles());
            String fallbackContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/InMemoryUserConfig.ftl", fallbackModel);
            files.add(new FilePreview(basePath + "security/InMemoryUserConfig.java", fallbackContent, "java"));
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

        // 4. Dynamic RBAC Mode: Generate Role JPA Entity + Admin API
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

            // Admin API: DataInitializer, RoleDto, RoleService, RoleController, UserRoleController
            Map<String, Object> adminModel = new HashMap<>();
            adminModel.put("packageName", request.getPackageName());
            adminModel.put("security", security);

            // Determine principal entity PK type for UserRoleController
            String pkType = "Long"; // default
            if (security.getPrincipalEntity() != null && request.getTables() != null) {
                Table principalTable = request.getTables().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(security.getPrincipalEntity()))
                    .findFirst()
                    .orElse(null);
                if (principalTable != null) {
                    adminModel.put("principalTable", principalTable);
                    pkType = principalTable.getColumns().stream()
                        .filter(c -> c.isPrimaryKey())
                        .map(c -> c.getJavaType())
                        .findFirst()
                        .orElse("Long");
                }
            }
            adminModel.put("pkType", pkType);

            // DataInitializer - seeds default roles and admin user
            String dataInitContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/DataInitializer.ftl", adminModel);
            files.add(new FilePreview(basePath + "config/DataInitializer.java", dataInitContent, "java"));

            // RoleDto
            String roleDtoContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/RoleDto.ftl", adminModel);
            files.add(new FilePreview(basePath + "dto/RoleDto.java", roleDtoContent, "java"));

            // RoleService
            String roleServiceContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/RoleService.ftl", adminModel);
            files.add(new FilePreview(basePath + "service/RoleService.java", roleServiceContent, "java"));

            // RoleController (admin API for role CRUD)
            String roleControllerContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/RoleController.ftl", adminModel);
            files.add(new FilePreview(basePath + "controller/RoleController.java", roleControllerContent, "java"));

            // UserRoleController (admin API for user-role assignments)
            String userRoleControllerContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/UserRoleController.ftl", adminModel);
            files.add(new FilePreview(basePath + "controller/UserRoleController.java", userRoleControllerContent, "java"));
        }

        // 5. JWT Components
        if ("JWT".equalsIgnoreCase(security.getAuthenticationType())) {
            Map<String, Object> jwtModel = new HashMap<>();
            jwtModel.put("request", request);
            jwtModel.put("packageName", request.getPackageName());
            jwtModel.put("security", security);

            // Find principal table for dynamic template generation (RegisterRequest, AuthController)
            if (security.getPrincipalEntity() != null && request.getTables() != null) {
                Table principalTable = request.getTables().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(security.getPrincipalEntity()))
                    .findFirst()
                    .orElse(null);
                if (principalTable != null) {
                    jwtModel.put("principalTable", principalTable);
                }
            }
            
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

        // 6. Password Reset (if enabled)
        if (security.isPasswordResetEnabled() && security.getPrincipalEntity() != null) {
            Map<String, Object> resetModel = new HashMap<>();
            resetModel.put("packageName", request.getPackageName());
            resetModel.put("principalEntity", security.getPrincipalEntity());
            resetModel.put("usernameField", security.getUsernameField());
            resetModel.put("passwordField", security.getPasswordField());
            resetModel.put("passwordResetTokenField", security.getPasswordResetTokenField() != null ? security.getPasswordResetTokenField() : "resetToken");
            resetModel.put("passwordResetExpiryField", security.getPasswordResetExpiryField() != null ? security.getPasswordResetExpiryField() : "resetTokenExpiry");

            String resetServiceContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/PasswordResetService.ftl", resetModel);
            files.add(new FilePreview(basePath + "security/PasswordResetService.java", resetServiceContent, "java"));

            String resetControllerContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/PasswordResetController.ftl", resetModel);
            files.add(new FilePreview(basePath + "security/PasswordResetController.java", resetControllerContent, "java"));

            String mailServiceContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/MailService.ftl", resetModel);
            files.add(new FilePreview(basePath + "security/MailService.java", mailServiceContent, "java"));
        }

        // 7. Refresh Token Persistence (if enabled)
        if (security.isRefreshTokenPersisted() && security.getPrincipalEntity() != null) {
            Map<String, Object> refreshModel = new HashMap<>();
            refreshModel.put("packageName", request.getPackageName());
            refreshModel.put("principalEntity", security.getPrincipalEntity());

            String refreshEntityContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/RefreshTokenEntity.ftl", refreshModel);
            files.add(new FilePreview(basePath + "security/RefreshToken.java", refreshEntityContent, "java"));

            String refreshRepoContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/RefreshTokenRepository.ftl", refreshModel);
            files.add(new FilePreview(basePath + "security/RefreshTokenRepository.java", refreshRepoContent, "java"));

            String refreshServiceContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/RefreshTokenService.ftl", refreshModel);
            files.add(new FilePreview(basePath + "security/RefreshTokenService.java", refreshServiceContent, "java"));
        }

        // 8. Social Login Support (if any social providers configured)
        boolean hasSocialLogins = security.getSocialLogins() != null && !security.getSocialLogins().isEmpty();
        if (hasSocialLogins && security.getPrincipalEntity() != null) {
            Map<String, Object> socialModel = new HashMap<>();
            socialModel.put("packageName", request.getPackageName());
            socialModel.put("security", security);
            socialModel.put("principalEntity", security.getPrincipalEntity());
            socialModel.put("usernameField", security.getUsernameField());
            socialModel.put("passwordField", security.getPasswordField());

            String oauthServiceContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/OAuth2UserService.ftl", socialModel);
            files.add(new FilePreview(basePath + "security/CustomOAuth2UserService.java", oauthServiceContent, "java"));

            // OAuth2LoginConfig (client registration for social providers)
            String oauthConfigContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/OAuth2LoginConfig.ftl", socialModel);
            files.add(new FilePreview(basePath + "config/OAuth2LoginConfig.java", oauthConfigContent, "java"));

            // Social auth controller (for JWT token exchange after OAuth2 callback)
            if ("JWT".equalsIgnoreCase(security.getAuthenticationType())) {
                String socialControllerContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/SocialAuthController.ftl", socialModel);
                files.add(new FilePreview(basePath + "controller/SocialAuthController.java", socialControllerContent, "java"));
            }
        }

        // 9. Form-Based Login components (if FORM_LOGIN auth type)
        if ("FORM_LOGIN".equalsIgnoreCase(security.getAuthenticationType())) {
            Map<String, Object> formModel = new HashMap<>();
            formModel.put("packageName", request.getPackageName());
            formModel.put("security", security);

            // FormLoginSecurityConfig (overrides default SecurityConfig for form-based auth)
            String formSecConfigContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/FormLoginSecurityConfig.ftl", formModel);
            files.add(new FilePreview(basePath + "config/FormLoginSecurityConfig.java", formSecConfigContent, "java"));

            // MVC Authentication Controller (login/logout pages)
            String authMvcContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/AuthenticationController.ftl", formModel);
            files.add(new FilePreview(basePath + "controller/AuthenticationController.java", authMvcContent, "java"));

            // Thymeleaf login template
            String loginHtmlContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/login.html.ftl", formModel);
            files.add(new FilePreview("src/main/resources/templates/login.html", loginHtmlContent, "html"));

            // Registration Controller (if registration enabled)
            boolean regEnabled = security.isRegistrationEnabled();
            if (regEnabled && security.getPrincipalEntity() != null) {
                String regControllerContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/RegistrationController.ftl", formModel);
                files.add(new FilePreview(basePath + "controller/RegistrationController.java", regControllerContent, "java"));
            }
        }

        // 10. Keycloak OAuth/OIDC components (if KEYCLOAK_OAUTH auth type)
        if ("KEYCLOAK_OAUTH".equalsIgnoreCase(security.getAuthenticationType())) {
            Map<String, Object> kcModel = new HashMap<>();
            kcModel.put("packageName", request.getPackageName());
            kcModel.put("security", security);

            // KeycloakOAuthConfig
            String kcOAuthContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/KeycloakOAuthConfig.ftl", kcModel);
            files.add(new FilePreview(basePath + "config/KeycloakOAuthConfig.java", kcOAuthContent, "java"));

            // UserSynchronizationService (sync Keycloak users to local DB)
            if (security.getPrincipalEntity() != null) {
                String userSyncContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/UserSynchronizationService.ftl", kcModel);
                files.add(new FilePreview(basePath + "service/UserSynchronizationService.java", userSyncContent, "java"));
            }
        }

        // 11. Keycloak realm export and docker-compose (if any Keycloak mode)
        boolean isKeycloak = "KEYCLOAK_RS".equalsIgnoreCase(security.getAuthenticationType())
                || "KEYCLOAK_OAUTH".equalsIgnoreCase(security.getAuthenticationType());
        if (isKeycloak) {
            Map<String, Object> kcDockerModel = new HashMap<>();
            kcDockerModel.put("security", security);

            // keycloak-realm.json
            String realmContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/keycloak-realm.json.ftl", kcDockerModel);
            files.add(new FilePreview("src/main/resources/keycloak-realm.json", realmContent, "json"));

            // docker-compose.keycloak.yml
            String kcComposeContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/docker-compose.keycloak.yml.ftl", kcDockerModel);
            files.add(new FilePreview("docker-compose.keycloak.yml", kcComposeContent, "yaml"));
        }

        // 12. Integration Test Helpers (if test users enabled)
        if (security.isTestUsersEnabled()) {
            Map<String, Object> testModel = new HashMap<>();
            testModel.put("packageName", request.getPackageName());
            testModel.put("security", security);

            // BaseIT - Base integration test class
            String baseItContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/BaseIT.ftl", testModel);
            String testBasePath = "src/test/java/" + request.getPackageName().replace(".", "/") + "/";
            files.add(new FilePreview(testBasePath + "BaseIT.java", baseItContent, "java"));

            // SecurityTestConfig
            String secTestConfigContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/SecurityTestConfig.ftl", testModel);
            files.add(new FilePreview(testBasePath + "config/SecurityTestConfig.java", secTestConfigContent, "java"));

            // Test user SQL seed script
            String testUsersContent = templateService.processTemplateToString(TEMPLATE_DIR + "security/test-users.sql.ftl", testModel);
            files.add(new FilePreview("src/test/resources/test-users.sql", testUsersContent, "sql"));
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
     * Generates database migration files (Flyway or Liquibase).
     */
    private List<FilePreview> generateMigrationFiles(ProjectRequest request, SpringConfig springConfig) {
        List<FilePreview> files = new ArrayList<>();
        String migrationTool = springConfig.getMigrationTool();

        Map<String, Object> model = new HashMap<>();
        model.put("tables", request.getTables() != null ? request.getTables() : new ArrayList<>());
        model.put("request", request);

        if ("flyway".equalsIgnoreCase(migrationTool)) {
            String content = templateService.processTemplateToString(TEMPLATE_DIR + "migration/V1__init_schema.sql.ftl", model);
            files.add(new FilePreview("src/main/resources/db/migration/V1__init_schema.sql", content, "sql"));
        } else if ("liquibase".equalsIgnoreCase(migrationTool)) {
            String masterContent = templateService.processTemplateToString(TEMPLATE_DIR + "migration/db.changelog-master.xml.ftl", model);
            files.add(new FilePreview("src/main/resources/db/changelog/db.changelog-master.xml", masterContent, "xml"));

            String changesetContent = templateService.processTemplateToString(TEMPLATE_DIR + "migration/001-init-schema.xml.ftl", model);
            files.add(new FilePreview("src/main/resources/db/changelog/001-init-schema.xml", changesetContent, "xml"));
        }

        return files;
    }

    /**
     * Generates Testcontainers configuration and base test class.
     */
    private List<FilePreview> generateTestcontainersFiles(ProjectRequest request) {
        List<FilePreview> files = new ArrayList<>();
        String testBasePath = "src/test/java/" + request.getPackageName().replace(".", "/") + "/";

        Map<String, Object> model = new HashMap<>();
        model.put("packageName", request.getPackageName());
        model.put("request", request);
        model.put("databaseType", request.getDatabaseType() != null ? request.getDatabaseType() : "h2");

        // TestcontainersConfig
        String configContent = templateService.processTemplateToString(TEMPLATE_DIR + "TestcontainersConfig.ftl", model);
        files.add(new FilePreview(testBasePath + "TestcontainersConfig.java", configContent, "java"));

        // TestcontainersTest base class
        String baseTestContent = templateService.processTemplateToString(TEMPLATE_DIR + "TestcontainersTest.ftl", model);
        files.add(new FilePreview(testBasePath + "TestcontainersTest.java", baseTestContent, "java"));

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
    /**
     * Generates an e2e.http file for testing the endpoints.
     */
    private FilePreview generateE2EHttp(ProjectRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("request", request);

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "e2e.http.ftl", model);
        return new FilePreview("src/test/e2e.http", content, "http");
    }}
