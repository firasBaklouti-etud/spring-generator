package com.firas.generator.model.config;

/**
 * Node.js (Express) specific configuration for project generation.
 * 
 * Contains Node.js and NPM-specific settings that don't apply
 * to other technology stacks.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public class NodeConfig {
    
    /** Node.js version to use (e.g., "18", "20", "22") */
    private String nodeVersion = "20";
    
    /** Package manager: "npm", "yarn", or "pnpm" */
    private String packageManager = "npm";
    
    /** Whether to use TypeScript instead of JavaScript */
    private boolean useTypeScript = true;
    
    /** ORM to use: "prisma", "sequelize", "typeorm" */
    private String orm = "prisma";
    
    /** Whether to use ESLint for linting */
    private boolean useEslint = true;
    
    /** Whether to use Prettier for formatting */
    private boolean usePrettier = true;
    
    /** Express version (e.g., "4.18.2") */
    private String expressVersion = "4.18.2";

    // Constructors
    public NodeConfig() {}

    // Getters and Setters
    public String getNodeVersion() { return nodeVersion; }
    public void setNodeVersion(String nodeVersion) { this.nodeVersion = nodeVersion; }

    public String getPackageManager() { return packageManager; }
    public void setPackageManager(String packageManager) { this.packageManager = packageManager; }

    public boolean isUseTypeScript() { return useTypeScript; }
    public void setUseTypeScript(boolean useTypeScript) { this.useTypeScript = useTypeScript; }

    public String getOrm() { return orm; }
    public void setOrm(String orm) { this.orm = orm; }

    public boolean isUseEslint() { return useEslint; }
    public void setUseEslint(boolean useEslint) { this.useEslint = useEslint; }

    public boolean isUsePrettier() { return usePrettier; }
    public void setUsePrettier(boolean usePrettier) { this.usePrettier = usePrettier; }

    public String getExpressVersion() { return expressVersion; }
    public void setExpressVersion(String expressVersion) { this.expressVersion = expressVersion; }
}
