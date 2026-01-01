package ${packageName}.security;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Enumeration of all roles in the system.
 * Each role has a set of permissions.
 */
public enum Role {
<#list definedRoles as role>
    ${role.name}(<#list role.permissions as perm>Permission.${perm}<#if perm?has_next>, </#if></#list>)<#if role?has_next>,<#else>;</#if>
</#list>

    private final Set<Permission> permissions;

    Role(Permission... permissions) {
        this.permissions = Set.of(permissions);
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public Set<String> getAuthorities() {
        return permissions.stream()
                .map(Permission::getAuthority)
                .collect(Collectors.toSet());
    }
}
