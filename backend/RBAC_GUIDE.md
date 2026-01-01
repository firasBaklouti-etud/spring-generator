# Security & RBAC Implementation Guide

## Overview

The Spring Generator now supports comprehensive **Dual-Mode Role-Based Access Control (RBAC)**, allowing you to choose between:

1. **Static RBAC (Enum-Based)**: Permissions and roles are defined as Java enums at compile-time
2. **Dynamic RBAC (Database-Driven)**: Permissions and roles are stored in the database and can be modified at runtime

## Architecture

### Security Configuration Model

The `SecurityConfig` class (in `model/config/SecurityConfig.java`) contains:

```java
// Authentication
private boolean enabled;
private String authenticationType; // "BASIC", "JWT", "OAUTH2"

// Principal Entity
private String principalEntity;    // e.g., "User"
private String usernameField;      // e.g., "email"
private String passwordField;      // e.g., "password"

// RBAC Configuration
private String rbacMode;           // "STATIC" or "DYNAMIC"
private List<String> permissions;  // e.g., ["USER_READ", "PRODUCT_WRITE"]
private List<RoleDefinition> definedRoles; // Role definitions

public static class RoleDefinition {
    private String name;           // e.g., "ADMIN"
    private List<String> permissions; // Assigned permissions
}
```

### Static RBAC Mode

When `rbacMode = "STATIC"`, the generator creates:

#### 1. Permission.java (Enum)
```java
package com.example.security;

public enum Permission {
    USER_READ,
    USER_WRITE,
    USER_DELETE,
    PRODUCT_READ,
    PRODUCT_WRITE;
    
    public String getAuthority() {
        return this.name();
    }
}
```

#### 2. Role.java (Enum)
```java
package com.example.security;

public enum Role {
    USER(Permission.USER_READ, Permission.PRODUCT_READ),
    ADMIN(Permission.USER_READ, Permission.USER_WRITE, Permission.USER_DELETE, 
          Permission.PRODUCT_READ, Permission.PRODUCT_WRITE);
    
    private final Set<Permission> permissions;
    
    Role(Permission... permissions) {
        this.permissions = Set.of(permissions);
    }
    
    public Set<String> getAuthorities() {
        return permissions.stream()
                .map(Permission::getAuthority)
                .collect(Collectors.toSet());
    }
}
```

#### 3. User Entity (UserDetails Implementation)
```java
@Entity
public class User implements UserDetails {
    // ... fields
    
    private Role role; // Enum field
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.role.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
```

### Dynamic RBAC Mode (In Progress)

When `rbacMode = "DYNAMIC"`, the generator will create:

1. **Role Entity**: JPA entity with `@ManyToMany` to User
2. **Permission Storage**: Either as `@ElementCollection<String>` or separate Permission entity
3. **User.getAuthorities()**: Flattens role permissions into authorities list

## Generation Flow

### SpringStackProvider Orchestration

```java
// 1. Metadata Injection (generateProject)
if (security.getPrincipalEntity() != null) {
    table.addMetadata("isUserDetails", true);
    table.addMetadata("rbacMode", security.getRbacMode());
    table.addMetadata("roleField", "role");
    // ... other metadata
}

// 2. Enum Generation (generateExtendedSecurityFiles)
if ("STATIC".equalsIgnoreCase(security.getRbacMode())) {
    // Generate Permission.java
    String permissionContent = templateService.processTemplateToString(
        "spring/Permission.ftl", rbacModel);
    files.add(new FilePreview("security/Permission.java", permissionContent));
    
    // Generate Role.java
    String roleContent = templateService.processTemplateToString(
        "spring/Role.ftl", rbacModel);
    files.add(new FilePreview("security/Role.java", roleContent));
}
```

### Entity.ftl Template Logic

```ftl
<#if isUserDetails?? && isUserDetails>
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        <#if rbacMode?? && rbacMode == "STATIC">
        // Static RBAC: Use Role Enum
        return this.${roleField}.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        <#elseif rbacMode?? && rbacMode == "DYNAMIC">
        // Dynamic RBAC: Use Role Entity
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (Role role : this.roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            for (String permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }
        return authorities;
        </#if>
    }
</#if>
```

## Frontend Integration

### Security Phase UI

The `security-phase.tsx` component provides:

1. **RBAC Mode Selector**: Toggle between Static and Dynamic
2. **Permission Auto-Generation**: Generates CRUD permissions from schema (e.g., `USER_READ`, `USER_WRITE`)
3. **Role Editor**: Define roles and assign permissions via checkbox matrix
4. **Resource Permissions**: Map roles to API endpoints

### State Management

The Zustand store (`store.ts`) maintains:

```typescript
interface SecurityConfig {
    rbacMode?: "STATIC" | "DYNAMIC";
    permissions?: string[];
    definedRoles?: { name: string, permissions: string[] }[];
    // ... other fields
}
```

## Usage Example

### 1. Configure in UI

1. Navigate to Security Phase
2. Select "Static (Enums)" mode
3. Permissions are auto-generated from tables
4. Define roles (e.g., USER, ADMIN) and assign permissions
5. Configure resource permissions per endpoint

### 2. Generated Code

The generator produces:
- `Permission.java` enum
- `Role.java` enum with permission sets
- `User.java` entity implementing `UserDetails`
- `SecurityConfig.java` with Spring Security configuration
- `CustomUserDetailsService.java` for authentication

### 3. Runtime Behavior

```java
// User has Role.ADMIN
user.getAuthorities() 
// Returns: ["USER_READ", "USER_WRITE", "USER_DELETE", "PRODUCT_READ", "PRODUCT_WRITE"]

// Spring Security checks
@PreAuthorize("hasAuthority('PRODUCT_WRITE')")
public Product updateProduct(@RequestBody Product product) {
    // Only users with PRODUCT_WRITE permission can access
}
```

## Best Practices

### When to Use Static RBAC
- Small to medium projects
- Fixed permission structure
- Performance-critical applications
- Compile-time type safety desired

### When to Use Dynamic RBAC
- Large enterprise applications
- Permissions change frequently
- Admin dashboard for role management needed
- Multi-tenant systems

### Permission Naming Convention
- Format: `{ENTITY}_{ACTION}`
- Examples: `USER_READ`, `PRODUCT_WRITE`, `ORDER_DELETE`
- Custom: `REPORT_EXPORT`, `ADMIN_DASHBOARD_ACCESS`

## Testing

Use the provided test scripts:

```powershell
# Update temp_request.json with your configuration
# Run verification
.\verify_generation.ps1
```

Check for:
- ✅ Permission.java generated
- ✅ Role.java generated
- ✅ User.java implements UserDetails
- ✅ getAuthorities() returns correct permissions

## Troubleshooting

### Enums Not Generated
- Verify `rbacMode` is set to `"STATIC"`
- Check `permissions` array is not empty
- Ensure `definedRoles` contains at least one role

### Frontend Not Sending RBAC Fields
- Check browser console for debug logs
- Verify `setSecurityConfig` is called with new fields
- Ensure `useEffect` in `security-phase.tsx` initializes data

## Future Enhancements

- [ ] Dynamic RBAC entity generation
- [ ] `@PreAuthorize` annotation generation for controllers
- [ ] Permission-to-endpoint mapping UI
- [ ] Role hierarchy support
- [ ] Permission inheritance

---

For implementation details, see:
- [RBAC Status](../RBAC_STATUS.md)
- [RBAC Walkthrough](../C:\Users\firas\.gemini\antigravity\brain\6e97d660-c449-4283-a3d9-cdd7db87c0e8\walkthrough.md)
