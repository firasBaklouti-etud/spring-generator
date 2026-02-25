package com.firas.generator.model.config;

/**
 * Configuration for frontend generation.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public class FrontendConfig {
    
    /** Whether frontend generation is enabled */
    private boolean enabled = false;
    
    /** Frontend framework: NEXTJS, ANGULAR, REACT */
    private String framework = "NEXTJS";
    
    /** Frontend dev server port */
    private int port = 3000;
    
    /** Backend API URL for the frontend to connect to */
    private String backendUrl = "http://localhost:8080";
    
    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getFramework() { return framework; }
    public void setFramework(String framework) { this.framework = framework; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getBackendUrl() { return backendUrl; }
    public void setBackendUrl(String backendUrl) { this.backendUrl = backendUrl; }
}
