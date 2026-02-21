package com.firas.generator.stack.spring;

import com.fasterxml.jackson.databind.JsonNode;
import com.firas.generator.model.DependencyGroup;
import com.firas.generator.model.DependencyMetadata;
import com.firas.generator.stack.DependencyProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dependency provider for Spring Boot.
 * 
 * Wraps the existing DependencyRegistry which fetches dependencies
 * from the Spring Initializr API (start.spring.io).
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
@Component
public class SpringDependencyProvider implements DependencyProvider {

    private static final Logger log = LoggerFactory.getLogger(SpringDependencyProvider.class);

    /** WebClient for making HTTP requests to Spring Initializr API */
    private final WebClient webClient = WebClient.create("https://start.spring.io");

    /** Map of dependency ID to dependency metadata for quick lookup */
    private final Map<String, DependencyMetadata> dependencyMap = new HashMap<>();

    /** List of all dependency groups */
    private final List<DependencyGroup> groups = new ArrayList<>();

    /**
     * Initializes the dependency registry by fetching data from Spring Initializr.
     * This method is called automatically after bean construction.
     */
    @PostConstruct
    public void initialize() {
        initializeDynamicDependencies();
    }

    /**
     * Fetches dependencies from Spring Initializr API dynamically.
     *
     * This method makes an HTTP request to the Spring Initializr metadata endpoint
     * to retrieve the latest available dependencies and their metadata. The dependencies
     * are organized into groups and stored in memory for quick access.
     *
     * @throws RuntimeException if the API request fails or returns invalid data
     */
    private void initializeDynamicDependencies() {
        try {
            JsonNode root = webClient.get()
                    .uri("/metadata/config")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null || !root.has("dependencies")) {
                throw new RuntimeException("Invalid response from Spring Initializr API");
            }

            JsonNode dependenciesNode = root.get("dependencies").get("content");
            for (JsonNode dependency : dependenciesNode) {
                DependencyGroup group = new DependencyGroup(dependency.get("name").asText());

                for (JsonNode dep : dependency.get("content")) {
                    DependencyMetadata dependencyMetadata=new DependencyMetadata();
                    dependencyMetadata.setId(dep.get("id")!=null?dep.get("id").asText():"");
                    dependencyMetadata.setName(dep.get("name")!=null?dep.get("name").asText():"");
                    dependencyMetadata.setArtifactId(dep.get("artifactId")!=null?dep.get("artifactId").asText():"");
                    dependencyMetadata.setVersion(dep.get("version")!=null?dep.get("version").asText():"");
                    dependencyMetadata.setGroupId(dep.get("groupId")!=null?dep.get("groupId").asText():"");
                    dependencyMetadata.setDescription(dep.get("description")!=null?dep.get("description").asText():"");
                    dependencyMetadata.setStarter(dep.get("starter")!=null && dep.get("starter").asBoolean(false));


                    group.addDependency(dependencyMetadata);
                    dependencyMap.put(dependencyMetadata.getId(), dependencyMetadata);
                }
                groups.add(group);

            }




        } catch (Exception e) {
            log.warn("Failed to fetch dependencies from Spring Initializr. " +
                    "The dependency listing feature will be unavailable until the API is reachable.", e);
        }
    }

    /**
     * Retrieves all dependency groups.
     *
     * @return List of all dependency groups with their associated dependencies
     */
    public List<DependencyGroup> getAllGroups() {
        return groups;
    }

    /**
     * Retrieves a specific dependency by its ID.
     *
     * @param id The unique identifier of the dependency
     * @return The DependencyMetadata object, or null if not found
     */
    public DependencyMetadata getDependencyById(String id) {
        return dependencyMap.get(id);
    }

    @Override
    public boolean isInitialized() {
        // Check if dependency registry has loaded groups
        List<DependencyGroup> groups = getAllGroups();
        return groups != null && !groups.isEmpty();
    }

    @Override
    public void refresh() {
        initialize();
    }

}
