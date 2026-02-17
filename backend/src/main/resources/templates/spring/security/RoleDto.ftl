package ${packageName}.dto;

import java.util.Set;

/**
 * Data Transfer Object for Role management
 */
public class RoleDto {

    private Long id;
    private String name;
    private String description;
    private Set<String> permissions;

    public RoleDto() {
    }

    public RoleDto(Long id, String name, String description, Set<String> permissions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
