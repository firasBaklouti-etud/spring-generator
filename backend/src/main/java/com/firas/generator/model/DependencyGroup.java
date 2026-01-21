package com.firas.generator.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of related Spring Boot dependencies.
 * 
 * Dependencies are organized into logical groups (e.g., "Web", "Security", "SQL")
 * to make it easier for users to find and select related dependencies when
 * generating a Spring Boot project.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-01
 */
public class DependencyGroup {
    /** The name of this dependency group (e.g., "Web", "Security") */
    private String name;
    
    /** List of dependencies belonging to this group */
    private List<DependencyMetadata> dependencies = new ArrayList<>();

    /**
     * Default constructor.
     */
    public DependencyGroup() {
    }

    /**
     * Constructor with group name.
     * 
     * @param name The name of this dependency group
     */
    public DependencyGroup(String name) {
        this.name = name;
    }

    /**
     * Constructor with group name and dependencies.
     * 
     * @param name The name of this dependency group
     * @param dependencies Initial list of dependencies for this group
     */
    public DependencyGroup(String name, List<DependencyMetadata> dependencies) {
        this.name = name;
        this.dependencies = dependencies;
    }

    /**
     * Adds a dependency to this group.
     * 
     * @param dependency The dependency to add to this group
     */
    public void addDependency(DependencyMetadata dependency) {
        this.dependencies.add(dependency);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<DependencyMetadata> getDependencies() { return dependencies; }
    public void setDependencies(List<DependencyMetadata> dependencies) { this.dependencies = dependencies; }

    @Override
    public String toString() {
        return "DependencyGroup{" +
                "name='" + name + '\'' +
                ", dependencies=" + dependencies +
                '}';
    }
}
