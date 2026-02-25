package com.firas.generator.model.config;

import com.firas.generator.model.Table;

import java.util.List;

/**
 * Internal model representing a single microservice definition.
 * Used by MicroservicesGenerator; not part of API contract.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public class ServiceDefinition {
    
    private String serviceName;
    private String artifactId;
    private int port;
    private List<Table> tables;
    private String packageName;
    
    public ServiceDefinition() {}
    
    public ServiceDefinition(String serviceName, String artifactId, int port, List<Table> tables, String packageName) {
        this.serviceName = serviceName;
        this.artifactId = artifactId;
        this.port = port;
        this.tables = tables;
        this.packageName = packageName;
    }
    
    // Getters and Setters
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public List<Table> getTables() { return tables; }
    public void setTables(List<Table> tables) { this.tables = tables; }
    
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
}
