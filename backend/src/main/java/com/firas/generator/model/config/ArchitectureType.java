package com.firas.generator.model.config;

/**
 * Enum representing the architecture type for Spring Boot projects.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public enum ArchitectureType {
    
    MONOLITH("monolith", "Monolith"),
    MICROSERVICES("microservices", "Microservices");
    
    private final String id;
    private final String displayName;
    
    ArchitectureType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    
    public static ArchitectureType fromId(String id) {
        if (id == null) return MONOLITH;
        for (ArchitectureType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return MONOLITH;
    }
}
