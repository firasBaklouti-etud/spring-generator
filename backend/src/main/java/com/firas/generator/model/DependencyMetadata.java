package com.firas.generator.model;

/**
 * Represents metadata for a Spring Boot dependency.
 * 
 * This class contains all the information needed to include a dependency in a Maven pom.xml file,
 * including Maven coordinates (groupId, artifactId, version), scope, and descriptive information
 * for display purposes.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-01
 */
public class DependencyMetadata {
    /** Unique identifier for this dependency (e.g., "web", "jpa", "security") */
    private String id;
    
    /** Human-readable name (e.g., "Spring Web", "Spring Data JPA") */
    private String name;
    
    /** Description of what this dependency provides */
    private String description;
    
    /** Maven groupId (e.g., "org.springframework.boot") */
    private String groupId;
    
    /** Maven artifactId (e.g., "spring-boot-starter-web") */
    private String artifactId;
    
    /** Version number (optional, may be managed by Spring Boot BOM) */
    private String version;
    
    /** Maven scope (e.g., "compile", "runtime", "provided", "test") */
    private String scope;
    
    /** Indicates if this is a Spring Boot starter dependency */
    private boolean isStarter;


    public DependencyMetadata() {
    }

    public DependencyMetadata(String id, String name, String description, String groupId, String artifactId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.isStarter = artifactId != null && artifactId.startsWith("spring-boot-starter-");
    }

    public DependencyMetadata(String id, String name, String description, String groupId, String artifactId, String version, String scope) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
        this.isStarter = artifactId != null && artifactId.startsWith("spring-boot-starter-");
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public boolean isStarter() { return isStarter; }
    public void setStarter(boolean starter) { isStarter = starter; }

    @Override
    public String toString() {
        return "DependencyMetadata{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", scope='" + scope + '\'' +
                ", isStarter=" + isStarter +
                '}';
    }
}
