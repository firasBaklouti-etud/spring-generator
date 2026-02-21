package com.firas.generator.stack.spring;

import com.firas.generator.model.FilePreview;
import com.firas.generator.model.Table;
import com.firas.generator.model.config.ProjectStructure;
import com.firas.generator.model.config.SecurityConfig;
import com.firas.generator.model.config.SecurityRule;
import com.firas.generator.model.config.SpringConfig;
import com.firas.generator.service.TemplateService;
import com.firas.generator.stack.CodeGenerator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Code generator for Spring Boot projects.
 * 
 * Generates JPA entities, Spring Data repositories, services, and REST controllers
 * from table metadata using FreeMarker templates.
 * 
 * Supports multiple project structures:
 * - LAYERED: Traditional folder-by-type (entity/, repository/, service/, controller/)
 * - FEATURE: Folder-by-feature (user/User.java, user/UserRepository.java, etc.)
 * - DDD: Domain-Driven Design (domain/user/entity/, domain/user/repository/)
 * - HEXAGONAL: Hexagonal/Clean Architecture (domain/model/, infrastructure/adapter/)
 * 
 * @author Firas Baklouti
 * @version 1.1
 * @since 2025-12-07
 */
@Component
public class SpringCodeGenerator implements CodeGenerator {
    
    private static final String TEMPLATE_DIR = "spring/";

    private final TemplateService templateService;

    // Thread-safe configuration holders (scoped per request)
    private final ThreadLocal<SecurityConfig> securityConfig = new ThreadLocal<>();

    private final ThreadLocal<SpringConfig> springConfig = new ThreadLocal<>();

    public SpringCodeGenerator(TemplateService templateService) {
        this.templateService = templateService;
    }

    public void setSecurityConfig(SecurityConfig config) {
        this.securityConfig.set(config);
    }

    public void setSpringConfig(SpringConfig config) {
        this.springConfig.set(config);
    }

    public void clearConfig() {
        this.securityConfig.remove();
        this.springConfig.remove();
    }

    /**
     * Gets the current project structure from springConfig, or LAYERED as default.
     */
    private ProjectStructure getProjectStructure() {
        SpringConfig config = springConfig.get();
        if (config != null && config.getProjectStructure() != null) {
            return config.getProjectStructure();
        }
        return ProjectStructure.LAYERED;
    }

    /**
     * Checks if the current database type is MongoDB.
     */
    private boolean isMongoDB() {
        SpringConfig config = springConfig.get();
        return config != null && "mongodb".equalsIgnoreCase(config.getDatabaseType());
    }

    /**
     * Checks if the current language is Kotlin.
     */
    private boolean isKotlin() {
        SpringConfig config = springConfig.get();
        return config != null && "kotlin".equalsIgnoreCase(config.getLanguage());
    }

    /**
     * Returns the file extension based on the current language.
     */
    private String getFileExtension() {
        return isKotlin() ? ".kt" : ".java";
    }
    
    /**
     * Generates the file path based on the project structure.
     * 
     * @param packageName Base package name
     * @param table The table/entity
     * @param fileType Type of file: "entity", "repository", "service", "controller", "dto", "mapper"
     * @param suffix Filename suffix (e.g., "Repository", "Service", "Controller")
     * @param isTest Whether this is a test file
     * @return The file path
     */
    private String generatePath(String packageName, Table table, String fileType, String suffix, boolean isTest) {
        String sourceDir = isKotlin() ? "src/main/kotlin/" : "src/main/java/";
        String testDir = isKotlin() ? "src/test/kotlin/" : "src/test/java/";
        String baseDir = isTest ? testDir : sourceDir;
        String packagePath = packageName.replace(".", "/");
        String className = table.getClassName();
        String featureName = className.toLowerCase();
        String fileName = className + suffix + getFileExtension();
        
        ProjectStructure structure = getProjectStructure();
        
        return switch (structure) {
            case LAYERED -> 
                // Traditional: entity/, repository/, service/, controller/
                baseDir + packagePath + "/" + fileType + "/" + fileName;
            
            case FEATURE -> 
                // Feature-based: user/User.java, user/UserRepository.java
                baseDir + packagePath + "/" + featureName + "/" + fileName;
            
            case DDD -> {
                // DDD: domain/user/entity/, domain/user/repository/, etc.
                // Controllers go in application/controller/
                if ("controller".equals(fileType)) {
                    yield baseDir + packagePath + "/application/controller/" + fileName;
                }
                yield baseDir + packagePath + "/domain/" + featureName + "/" + fileType + "/" + fileName;
            }
            
            case HEXAGONAL -> {
                // Hexagonal: domain/model/, infrastructure/adapter/
                yield switch (fileType) {
                    case "entity" -> 
                        baseDir + packagePath + "/domain/model/" + fileName;
                    case "repository" -> 
                        baseDir + packagePath + "/infrastructure/adapter/out/persistence/" + fileName;
                    case "service" -> 
                        baseDir + packagePath + "/application/service/" + fileName;
                    case "controller" -> 
                        baseDir + packagePath + "/infrastructure/adapter/in/web/" + fileName;
                    case "dto" -> 
                        baseDir + packagePath + "/application/dto/" + fileName;
                    case "mapper" -> 
                        baseDir + packagePath + "/application/mapper/" + fileName;
                    default -> 
                        baseDir + packagePath + "/" + fileType + "/" + fileName;
                };
            }
        };
    }
    
    /**
     * Gets the effective package name for the file based on structure.
     * This is used in templates to set the correct package declaration.
     */
    private String getEffectivePackage(String basePackage, Table table, String fileType) {
        String featureName = table.getClassName().toLowerCase();
        ProjectStructure structure = getProjectStructure();
        
        return switch (structure) {
            case LAYERED -> basePackage + "." + fileType;
            case FEATURE -> basePackage + "." + featureName;
            case DDD -> {
                if ("controller".equals(fileType)) {
                    yield basePackage + ".application.controller";
                }
                yield basePackage + ".domain." + featureName + "." + fileType;
            }
            case HEXAGONAL -> switch (fileType) {
                case "entity" -> basePackage + ".domain.model";
                case "repository" -> basePackage + ".infrastructure.adapter.out.persistence";
                case "service" -> basePackage + ".application.service";
                case "controller" -> basePackage + ".infrastructure.adapter.in.web";
                case "dto" -> basePackage + ".application.dto";
                case "mapper" -> basePackage + ".application.mapper";
                default -> basePackage + "." + fileType;
            };
        };
    }
    
    @Override
    public FilePreview generateEntity(Table table, String packageName) {
        String effectivePackage = getEffectivePackage(packageName, table, "entity");
        Map<String, Object> model = createModel(table, packageName, effectivePackage, "entity");
        
        String template;
        if (isKotlin()) {
            template = TEMPLATE_DIR + "kotlin/Entity.kt.ftl";
        } else if (isMongoDB()) {
            template = TEMPLATE_DIR + "MongoEntity.ftl";
        } else {
            template = TEMPLATE_DIR + "Entity.ftl";
        }
        String content = templateService.processTemplateToString(template, model);
        String path = generatePath(packageName, table, "entity", "", false);
        
        return new FilePreview(path, content, isKotlin() ? "kotlin" : "java");
    }
    
    @Override
    public FilePreview generateRepository(Table table, String packageName) {
        String effectivePackage = getEffectivePackage(packageName, table, "repository");
        Map<String, Object> model = createModel(table, packageName, effectivePackage, "repository");
        
        String template;
        if (isKotlin()) {
            template = TEMPLATE_DIR + "kotlin/Repository.kt.ftl";
        } else if (isMongoDB()) {
            template = TEMPLATE_DIR + "MongoRepository.ftl";
        } else {
            template = TEMPLATE_DIR + "Repository.ftl";
        }
        String content = templateService.processTemplateToString(template, model);
        String path = generatePath(packageName, table, "repository", "Repository", false);
        
        return new FilePreview(path, content, isKotlin() ? "kotlin" : "java");
    }
    
    @Override
    public FilePreview generateService(Table table, String packageName) {
        String effectivePackage = getEffectivePackage(packageName, table, "service");
        Map<String, Object> model = createModel(table, packageName, effectivePackage, "service");
        
        String template;
        if (isKotlin()) {
            template = TEMPLATE_DIR + "kotlin/Service.kt.ftl";
        } else if (isMongoDB()) {
            template = TEMPLATE_DIR + "MongoService.ftl";
        } else {
            template = TEMPLATE_DIR + "Service.ftl";
        }
        String content = templateService.processTemplateToString(template, model);
        String path = generatePath(packageName, table, "service", "Service", false);
        
        return new FilePreview(path, content, isKotlin() ? "kotlin" : "java");
    }
    
    @Override
    public FilePreview generateController(Table table, String packageName) {
        String effectivePackage = getEffectivePackage(packageName, table, "controller");
        Map<String, Object> model = createModel(table, packageName, effectivePackage, "controller");
        
        // Add security configuration to controller model
        SecurityConfig secConfig = securityConfig.get();
        if (secConfig != null && secConfig.isEnabled()) {
            model.put("securityEnabled", true);

            // Determine if annotations should be used
            boolean useAnnotations = "ANNOTATION".equalsIgnoreCase(secConfig.getSecurityStyle());
            model.put("useAnnotations", useAnnotations);

            // Filter security rules for this entity's endpoints
            // Use pluralized name to match the Controller's @RequestMapping URL pattern
            if (secConfig.getRules() != null) {
                String resourceName = table.getClassName().toLowerCase();
                if (!resourceName.endsWith("s")) {
                    resourceName = resourceName + "s";
                }
                String basePath = "/api/" + resourceName;
                List<SecurityRule> entityRules = secConfig.getRules().stream()
                    .filter(rule -> rule.getPath() != null &&
                            (rule.getPath().startsWith(basePath + "/") ||
                             rule.getPath().equals(basePath + "/**")))
                    .collect(Collectors.toList());
                model.put("securityRules", entityRules);
            }
        } else {
            model.put("securityEnabled", false);
        }
        
        String template = isKotlin() ? TEMPLATE_DIR + "kotlin/Controller.kt.ftl" : TEMPLATE_DIR + "Controller.ftl";
        String content = templateService.processTemplateToString(template, model);
        String path = generatePath(packageName, table, "controller", "Controller", false);
        
        return new FilePreview(path, content, isKotlin() ? "kotlin" : "java");
    }
    
    @Override
    public FilePreview generateDto(Table table, String packageName) {
        String effectivePackage = getEffectivePackage(packageName, table, "dto");
        Map<String, Object> model = createModel(table, packageName, effectivePackage, "dto");

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "Dto.ftl", model);
        String path = generatePath(packageName, table, "dto", "Dto", false);

        return new FilePreview(path, content, "java");
    }

    @Override
    public FilePreview generateMapper(Table table, String packageName) {
        String effectivePackage = getEffectivePackage(packageName, table, "mapper");
        Map<String, Object> model = createModel(table, packageName, effectivePackage, "mapper");

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "Mapper.ftl", model);
        String path = generatePath(packageName, table, "mapper", "Mapper", false);

        return new FilePreview(path, content, "java");
    }

    /**
     * Generates a MapStruct-based mapper interface for a table.
     */
    public FilePreview generateMapStructMapper(Table table, String packageName) {
        String effectivePackage = getEffectivePackage(packageName, table, "mapper");
        Map<String, Object> model = createModel(table, packageName, effectivePackage, "mapper");

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "MapStructMapper.ftl", model);
        String path = generatePath(packageName, table, "mapper", "Mapper", false);

        return new FilePreview(path, content, "java");
    }
    
    /**
     * Generates a JUnit test for the repository layer.
     */
    public FilePreview generateRepositoryTest(Table table, String packageName) {
        String effectivePackage = getEffectivePackage(packageName, table, "repository");
        Map<String, Object> model = createModel(table, packageName, effectivePackage, "repository");
        
        String content = templateService.processTemplateToString(TEMPLATE_DIR + "RepositoryTest.ftl", model);
        String path = generatePath(packageName, table, "repository", "RepositoryTest", true);
        
        return new FilePreview(path, content, "java");
    }
    
    /**
     * Generates a JUnit test for the controller layer using MockMvc.
     */
    public FilePreview generateControllerTest(Table table, String packageName) {
        String effectivePackage = getEffectivePackage(packageName, table, "controller");
        Map<String, Object> model = createModel(table, packageName, effectivePackage, "controller");

        // Add security configuration for test generation
        SecurityConfig secConfig = securityConfig.get();
        if (secConfig != null && secConfig.isEnabled()) {
            model.put("securityEnabled", true);
            model.put("authType", secConfig.getAuthenticationType());
        } else {
            model.put("securityEnabled", false);
        }

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "ControllerTest.ftl", model);
        String path = generatePath(packageName, table, "controller", "ControllerTest", true);

        return new FilePreview(path, content, "java");
    }

    /**
     * Generates a Rest-Assured based controller test for a table.
     */
    public FilePreview generateRestAssuredTest(Table table, String packageName) {
        String effectivePackage = getEffectivePackage(packageName, table, "controller");
        Map<String, Object> model = createModel(table, packageName, effectivePackage, "controller");

        SecurityConfig secConfig = securityConfig.get();
        if (secConfig != null && secConfig.isEnabled()) {
            model.put("securityEnabled", true);
            model.put("authType", secConfig.getAuthenticationType());
        } else {
            model.put("securityEnabled", false);
        }

        String content = templateService.processTemplateToString(TEMPLATE_DIR + "RestAssuredTest.ftl", model);
        String path = generatePath(packageName, table, "controller", "RestAssuredTest", true);

        return new FilePreview(path, content, "java");
    }
    
    /**
     * Creates the template data model for a table with structure-aware package.
     * 
     * @param table The table metadata
     * @param basePackageName The base package name from project configuration
     * @param effectivePackage The effective package based on project structure
     * @param fileType The type of file being generated
     * @return Model map for template processing
     */
    private Map<String, Object> createModel(Table table, String basePackageName, String effectivePackage, String fileType) {
        Map<String, Object> model = new HashMap<>();
        model.put("table", table);
        model.put("packageName", effectivePackage);
        model.put("basePackageName", basePackageName);
        model.put("projectStructure", getProjectStructure().getId());

        // Add effective packages for all layers (for cross-layer imports)
        model.put("entityPackage", getEffectivePackage(basePackageName, table, "entity"));
        model.put("repositoryPackage", getEffectivePackage(basePackageName, table, "repository"));
        model.put("servicePackage", getEffectivePackage(basePackageName, table, "service"));
        model.put("controllerPackage", getEffectivePackage(basePackageName, table, "controller"));
        model.put("dtoPackage", getEffectivePackage(basePackageName, table, "dto"));
        model.put("mapperPackage", getEffectivePackage(basePackageName, table, "mapper"));

        if (table.getMetadata() != null) {
            model.putAll(table.getMetadata());
        }
        return model;
    }
}
