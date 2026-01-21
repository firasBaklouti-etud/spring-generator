package com.firas.generator.model;

/**
 * Enumeration of JPA relationship types.
 * 
 * Defines the four types of relationships that can exist between JPA entities,
 * corresponding to the standard JPA annotations.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-01
 */
public enum RelationshipType {
    /** One entity instance relates to many instances of another entity */
    ONE_TO_MANY,
    
    /** Many entity instances relate to one instance of another entity */
    MANY_TO_ONE,
    
    /** One entity instance relates to exactly one instance of another entity */
    ONE_TO_ONE,
    
    /** Many entity instances relate to many instances of another entity */
    MANY_TO_MANY
}
