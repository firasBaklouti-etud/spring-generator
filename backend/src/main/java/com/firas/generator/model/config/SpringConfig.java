package com.firas.generator.model.config;

/**
 * Spring Boot specific configuration for project generation.
 * 
 * Contains Maven/Gradle and Spring-specific settings that don't apply
 * to other technology stacks.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public class SpringConfig {
    
    /** Maven groupId for the generated project (e.g., "com.example") */
    private String groupId = "com.example";
    
    /** Maven artifactId for the generated project (e.g., "demo") */
    private String artifactId = "demo";
    
    /** Java version to use (e.g., "17", "21") */
    private String javaVersion = "17";
    
    /** Spring Boot version to use (e.g., "3.2.0") */
    private String bootVersion = "3.2.0";
    
    /** Build tool: "maven" or "gradle" */
    private String buildTool = "maven";
    
    /** Packaging type: "jar" or "war" */
    private String packaging = "jar";

    // Constructors
    public SpringConfig() {}
    
    public SpringConfig(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    // Getters and Setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

    public String getBootVersion() { return bootVersion; }
    public void setBootVersion(String bootVersion) { this.bootVersion = bootVersion; }

    public String getBuildTool() { return buildTool; }
    public void setBuildTool(String buildTool) { this.buildTool = buildTool; }

    public String getPackaging() { return packaging; }
    public void setPackaging(String packaging) { this.packaging = packaging; }
}
