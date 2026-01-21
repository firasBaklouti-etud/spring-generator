package com.firas.generator.stack;

/**
 * Enum representing the available technology stacks for code generation.
 * 
 * Each stack type includes metadata about the stack's identifier, display name,
 * programming language, and default runtime version.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public enum StackType {
    
    /** Spring Boot with Java/Maven */
    SPRING("spring", "Spring Boot", "java", "17"),
    
    /** Node.js with Express (future) */
    NODE("node", "Node.js Express", "javascript", "20"),
    
    /** NestJS with TypeScript (future) */
    NEST("nest", "NestJS", "typescript", "20"),
    
    /** FastAPI with Python (future) */
    FASTAPI("fastapi", "FastAPI", "python", "3.11");
    
    /** Unique identifier for the stack (used in API requests) */
    private final String id;
    
    /** Human-readable display name */
    private final String displayName;
    
    /** Primary programming language */
    private final String language;
    
    /** Default runtime/language version */
    private final String defaultVersion;
    
    StackType(String id, String displayName, String language, String defaultVersion) {
        this.id = id;
        this.displayName = displayName;
        this.language = language;
        this.defaultVersion = defaultVersion;
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getLanguage() { return language; }
    public String getDefaultVersion() { return defaultVersion; }
    
    /**
     * Find a StackType by its ID (case-insensitive).
     * 
     * @param id The stack identifier
     * @return The matching StackType
     * @throws IllegalArgumentException if no matching stack is found
     */
    public static StackType fromId(String id) {
        for (StackType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown stack type: " + id);
    }
}
