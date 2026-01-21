package com.firas.generator.model;

import com.firas.generator.model.config.*;
import com.firas.generator.stack.StackType;

import java.util.List;

/**
 * Represents a request to generate a project for any supported technology stack.
 * 
 * This class uses composition to separate common fields from stack-specific configuration:
 * - Common fields (name, description, packageName, tables, etc.) apply to all stacks
 * - Stack-specific configuration is encapsulated in dedicated config objects
 * 
 * The stackType field determines which technology stack to generate (Spring, Node, etc.).
 * If not specified, defaults to SPRING for backward compatibility.
 * 
 * For backward compatibility, legacy Spring fields (groupId, artifactId, etc.) are still
 * accepted and automatically merged into springConfig.
 * 
 * @author Firas Baklouti
 * @version 3.0
 * @since 2025-12-01
 */
public class ProjectRequest {
    
    // ==================== Stack Routing ====================
    
    /** Technology stack to generate (defaults to SPRING for backward compatibility) */
    private StackType stackType = StackType.SPRING;
    
    // ==================== Common Fields (All Stacks) ====================
    
    /** Human-readable project name */
    private String name;
    
    /** Project description */
    private String description;
    
    /** Base package/module name (e.g., "com.example.demo" for Java, "my-app" for Node) */
    private String packageName;
    
    /** List of dependencies to include in the project */
    private List<DependencyMetadata> dependencies;
    
    /** Database tables for CRUD generation */
    private List<Table> tables;
    
    /** Database type (mysql, postgresql, mariadb, sqlite, sqlserver) */
    private String databaseType;
    
    // ==================== Code Generation Flags (All Stacks) ====================
    
    /** Flag to include entity/model classes */
    private boolean includeEntity;
    
    /** Flag to include repository/data access layer */
    private boolean includeRepository;
    
    /** Flag to include service/business logic layer */
    private boolean includeService;
    
    /** Flag to include controller/router layer */
    private boolean includeController;
    
    /** Flag to include DTO (Data Transfer Object) classes */
    private boolean includeDto;
    
    /** Flag to include mapper classes for entity-DTO conversion */
    private boolean includeMapper;
    
    /** Flag to include JUnit tests for repository and controller layers */
    private boolean includeTests;
    
    /** Flag to include Docker files (Dockerfile, docker-compose.yml) */
    private boolean includeDocker;
    
    // ==================== Stack-Specific Configurations ====================
    
    /** Spring Boot specific configuration */
    private SpringConfig springConfig;
    
    /** Node.js/Express specific configuration */
    private NodeConfig nodeConfig;
    
    /** NestJS specific configuration */
    private NestConfig nestConfig;
    
    /** FastAPI/Python specific configuration */
    private FastAPIConfig fastapiConfig;

    /** Security configuration */
    private SecurityConfig securityConfig;

    
    // ==================== Legacy Fields (Backward Compatibility) ====================
    // These fields are kept for backward compatibility with existing frontend
    // They map to springConfig fields
    
    /** @deprecated Use springConfig.groupId instead */
    private String groupId;
    
    /** @deprecated Use springConfig.artifactId instead */
    private String artifactId;
    
    /** @deprecated Use springConfig.javaVersion instead */
    private String javaVersion;
    
    /** @deprecated Use springConfig.bootVersion instead */
    private String bootVersion;
    
    // ==================== Getters and Setters ====================
    
    public StackType getStackType() { return stackType; }
    public void setStackType(StackType stackType) { this.stackType = stackType; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public List<DependencyMetadata> getDependencies() { return dependencies; }
    public void setDependencies(List<DependencyMetadata> dependencies) { this.dependencies = dependencies; }

    public List<Table> getTables() { return tables; }
    public void setTables(List<Table> tables) { this.tables = tables; }
    
    public String getDatabaseType() { return databaseType; }
    public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }
    
    public boolean isIncludeEntity() { return includeEntity; }
    public void setIncludeEntity(boolean includeEntity) { this.includeEntity = includeEntity; }

    public boolean isIncludeRepository() { return includeRepository; }
    public void setIncludeRepository(boolean includeRepository) { this.includeRepository = includeRepository; }

    public boolean isIncludeService() { return includeService; }
    public void setIncludeService(boolean includeService) { this.includeService = includeService; }

    public boolean isIncludeController() { return includeController; }
    public void setIncludeController(boolean includeController) { this.includeController = includeController; }

    public boolean isIncludeDto() { return includeDto; }
    public void setIncludeDto(boolean includeDto) { this.includeDto = includeDto; }

    public boolean isIncludeMapper() { return includeMapper; }
    public void setIncludeMapper(boolean includeMapper) { this.includeMapper = includeMapper; }
    
    public boolean isIncludeTests() { return includeTests; }
    public void setIncludeTests(boolean includeTests) { this.includeTests = includeTests; }
    
    public boolean isIncludeDocker() { return includeDocker; }
    public void setIncludeDocker(boolean includeDocker) { this.includeDocker = includeDocker; }
    
    // Stack-specific config getters/setters
    
    public SpringConfig getSpringConfig() { return springConfig; }
    public void setSpringConfig(SpringConfig springConfig) { this.springConfig = springConfig; }
    
    public NodeConfig getNodeConfig() { return nodeConfig; }
    public void setNodeConfig(NodeConfig nodeConfig) { this.nodeConfig = nodeConfig; }
    
    public NestConfig getNestConfig() { return nestConfig; }
    public void setNestConfig(NestConfig nestConfig) { this.nestConfig = nestConfig; }
    
    public FastAPIConfig getFastapiConfig() { return fastapiConfig; }
    public void setFastapiConfig(FastAPIConfig fastapiConfig) { this.fastapiConfig = fastapiConfig; }

    public SecurityConfig getSecurityConfig() { return securityConfig; }
    public void setSecurityConfig(SecurityConfig securityConfig) { this.securityConfig = securityConfig; }

    
    // ==================== Legacy Field Getters (Backward Compatibility) ====================
    
    /**
     * Gets the groupId, checking both legacy field and springConfig.
     * @deprecated Use getSpringConfig().getGroupId() instead
     */
    public String getGroupId() { 
        if (groupId != null) return groupId;
        return springConfig != null ? springConfig.getGroupId() : null;
    }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    
    /**
     * Gets the artifactId, checking both legacy field and springConfig.
     * @deprecated Use getSpringConfig().getArtifactId() instead
     */
    public String getArtifactId() { 
        if (artifactId != null) return artifactId;
        return springConfig != null ? springConfig.getArtifactId() : null;
    }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }
    
    /**
     * Gets the javaVersion, checking both legacy field and springConfig.
     * @deprecated Use getSpringConfig().getJavaVersion() instead
     */
    public String getJavaVersion() { 
        if (javaVersion != null) return javaVersion;
        return springConfig != null ? springConfig.getJavaVersion() : "17";
    }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
    
    /**
     * Gets the bootVersion, checking both legacy field and springConfig.
     * @deprecated Use getSpringConfig().getBootVersion() instead
     */
    public String getBootVersion() { 
        if (bootVersion != null) return bootVersion;
        return springConfig != null ? springConfig.getBootVersion() : "3.2.0";
    }
    public void setBootVersion(String bootVersion) { this.bootVersion = bootVersion; }
    
    // ==================== Helper Methods ====================
    
    /**
     * Gets or creates the SpringConfig, merging legacy fields if present.
     * Useful for Spring provider to get a complete config object.
     */
    public SpringConfig getEffectiveSpringConfig() {
        SpringConfig config = springConfig != null ? springConfig : new SpringConfig();
        
        // Merge legacy fields if present
        if (groupId != null) config.setGroupId(groupId);
        if (artifactId != null) config.setArtifactId(artifactId);
        if (javaVersion != null) config.setJavaVersion(javaVersion);
        if (bootVersion != null) config.setBootVersion(bootVersion);
        
        return config;
    }
    
    /**
     * Gets or creates the appropriate config for the current stack type.
     */
    public Object getEffectiveConfig() {
        return switch (stackType) {
            case SPRING -> getEffectiveSpringConfig();
            case NODE -> nodeConfig != null ? nodeConfig : new NodeConfig();
            case NEST -> nestConfig != null ? nestConfig : new NestConfig();
            case FASTAPI -> fastapiConfig != null ? fastapiConfig : new FastAPIConfig();
        };
    }
}
