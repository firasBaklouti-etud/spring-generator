package com.firas.generator.model.config;

/**
 * NestJS specific configuration for project generation.
 * 
 * Contains NestJS and TypeScript-specific settings that don't apply
 * to other technology stacks. NestJS is always TypeScript-based.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public class NestConfig {
    
    /** Node.js version to use (e.g., "18", "20", "22") */
    private String nodeVersion = "20";
    
    /** Package manager: "npm", "yarn", or "pnpm" */
    private String packageManager = "npm";
    
    /** ORM to use: "typeorm", "prisma", "mikro-orm" */
    private String orm = "typeorm";
    
    /** NestJS version (e.g., "10.0.0") */
    private String nestVersion = "10.0.0";
    
    /** Whether to use strict mode TypeScript */
    private boolean strictMode = true;
    
    /** Whether to include Swagger/OpenAPI documentation */
    private boolean useSwagger = true;
    
    /** Whether to include class-validator for DTO validation */
    private boolean useValidation = true;

    // Constructors
    public NestConfig() {}

    // Getters and Setters
    public String getNodeVersion() { return nodeVersion; }
    public void setNodeVersion(String nodeVersion) { this.nodeVersion = nodeVersion; }

    public String getPackageManager() { return packageManager; }
    public void setPackageManager(String packageManager) { this.packageManager = packageManager; }

    public String getOrm() { return orm; }
    public void setOrm(String orm) { this.orm = orm; }

    public String getNestVersion() { return nestVersion; }
    public void setNestVersion(String nestVersion) { this.nestVersion = nestVersion; }

    public boolean isStrictMode() { return strictMode; }
    public void setStrictMode(boolean strictMode) { this.strictMode = strictMode; }

    public boolean isUseSwagger() { return useSwagger; }
    public void setUseSwagger(boolean useSwagger) { this.useSwagger = useSwagger; }

    public boolean isUseValidation() { return useValidation; }
    public void setUseValidation(boolean useValidation) { this.useValidation = useValidation; }
}
