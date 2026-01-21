package ${packageName}.security;

/**
 * Enumeration of all permissions in the system.
 * Generated based on entity CRUD operations and custom permissions.
 */
public enum Permission {
<#list permissions as permission>
    ${permission}<#if permission?has_next>,<#else>;</#if>
</#list>

    public String getAuthority() {
        return this.name();
    }
}
