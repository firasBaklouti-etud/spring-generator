package com.firas.generator.model.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for microservices architecture generation.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public class MicroservicesConfig {
    
    /** Mode: "AUTO" (one service per table) or "MANUAL" (user-defined grouping) */
    private String mode = "AUTO";
    
    /** For MANUAL mode: maps service names to table names */
    private Map<String, List<String>> serviceTableMapping = new HashMap<>();
    
    /** Eureka discovery server port */
    private int discoveryPort = 8761;
    
    /** Spring Cloud Config server port */
    private int configPort = 8888;
    
    /** API Gateway port */
    private int gatewayPort = 8080;
    
    /** Starting port for individual services */
    private int serviceStartPort = 8081;
    
    // Getters and Setters
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    
    public Map<String, List<String>> getServiceTableMapping() { return serviceTableMapping; }
    public void setServiceTableMapping(Map<String, List<String>> serviceTableMapping) { this.serviceTableMapping = serviceTableMapping; }
    
    public int getDiscoveryPort() { return discoveryPort; }
    public void setDiscoveryPort(int discoveryPort) { this.discoveryPort = discoveryPort; }
    
    public int getConfigPort() { return configPort; }
    public void setConfigPort(int configPort) { this.configPort = configPort; }
    
    public int getGatewayPort() { return gatewayPort; }
    public void setGatewayPort(int gatewayPort) { this.gatewayPort = gatewayPort; }
    
    public int getServiceStartPort() { return serviceStartPort; }
    public void setServiceStartPort(int serviceStartPort) { this.serviceStartPort = serviceStartPort; }
}
