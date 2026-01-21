package ${packageName}.entity;

import jakarta.persistence.*;
<#assign hasRelationships = (table.relationships?? && table.relationships?size > 0)>
<#assign hasCollections = false>
<#if hasRelationships>
<#list table.relationships as rel>
<#if rel.type == "ONE_TO_MANY" || rel.type == "MANY_TO_MANY">
<#assign hasCollections = true>
<#break>
</#if>
</#list>
</#if>
<#if hasCollections>
import java.util.List;
import java.util.ArrayList;
</#if>
import java.time.LocalDate;
import java.time.LocalDateTime;

<#if isUserDetails?? && isUserDetails>
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
</#if>
<#if isUserDetails?? && isUserDetails>
import com.fasterxml.jackson.annotation.JsonIgnore;
<#if rbacMode?? && rbacMode == "STATIC">
import ${packageName}.security.Role;
</#if>
<#if table.relationships?? && table.relationships?size &gt; 0>
import lombok.ToString; // Assuming Lombok is available/used or manual exclusion needed
</#if>
</#if>

@Entity
@Table(name = "${table.name}")
public class ${table.className} <#if isUserDetails?? && isUserDetails>implements UserDetails</#if> {

<#list table.columns as column>
    <#if !column.foreignKey>
    <#if column.primaryKey>
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    </#if>
    <#if isUserDetails?? && isUserDetails && column.fieldName == passwordField>
    @JsonIgnore
    // @ToString.Exclude // Uncomment if using Lombok
    </#if>
    @Column(name = "${column.name}")
    private ${column.javaType} ${column.fieldName};

    </#if>
</#list>

<#if hasRelationships>
<#list table.relationships as rel>
    <#if rel.type == "MANY_TO_ONE">
    @ManyToOne
    @JoinColumn(name = "${rel.sourceColumn}")
    private ${rel.targetClassName} ${rel.fieldName};

    <#elseif rel.type == "ONE_TO_MANY">
    @OneToMany(mappedBy = "${rel.mappedBy}")
    private List<${rel.targetClassName}> ${rel.fieldName} = new ArrayList<>();

    <#elseif rel.type == "ONE_TO_ONE">
    <#if rel.sourceColumn??>
    @OneToOne
    @JoinColumn(name = "${rel.sourceColumn}")
    private ${rel.targetClassName} ${rel.fieldName};

    <#else>
    @OneToOne(mappedBy = "${rel.mappedBy}")
    private ${rel.targetClassName} ${rel.fieldName};

    </#if>
    <#elseif rel.type == "MANY_TO_MANY">
    @ManyToMany(fetch = FetchType.EAGER) // Eager fetch for roles implies robust loading
    @JoinTable(
        name = "${rel.joinTable}",
        joinColumns = @JoinColumn(name = "${rel.sourceColumn}"),
        inverseJoinColumns = @JoinColumn(name = "${rel.targetColumn}")
    )
    private List<${rel.targetClassName}> ${rel.fieldName} = new ArrayList<>();

    </#if>
</#list>
</#if>

<#list table.columns as column>
    <#if !column.foreignKey>
    public ${column.javaType} get${column.fieldName?cap_first}() {
        return ${column.fieldName};
    }

    public void set${column.fieldName?cap_first}(${column.javaType} ${column.fieldName}) {
        this.${column.fieldName} = ${column.fieldName};
    }

    </#if>
</#list>

<#if hasRelationships>
<#list table.relationships as rel>
    <#if rel.type == "ONE_TO_MANY" || rel.type == "MANY_TO_MANY">
    public List<${rel.targetClassName}> get${rel.fieldName?cap_first}() {
        return ${rel.fieldName};
    }

    public void set${rel.fieldName?cap_first}(List<${rel.targetClassName}> ${rel.fieldName}) {
        this.${rel.fieldName} = ${rel.fieldName};
    }

    <#else>
    public ${rel.targetClassName} get${rel.fieldName?cap_first}() {
        return ${rel.fieldName};
    }

    public void set${rel.fieldName?cap_first}(${rel.targetClassName} ${rel.fieldName}) {
        this.${rel.fieldName} = ${rel.fieldName};
    }

    </#if>
</#list>
</#if>

<#if isUserDetails?? && isUserDetails>
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        <#if rbacMode?? && rbacMode == "STATIC">
        // Static RBAC Mode: Use Role Enum
        <#if roleField??>
        return this.${roleField}.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toList());
        <#else>
        // Fallback: Single role field as enum
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        </#if>
        <#elseif rbacMode?? && rbacMode == "DYNAMIC">
        // Dynamic RBAC Mode: Use Role Entity with ManyToMany relationship
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (${roleEntity} role : this.roles) {
            String roleName = role.getName().toUpperCase();
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
            authorities.add(new SimpleGrantedAuthority(roleName));
            
            // Add permissions if role has them
            if (role.getPermissions() != null) {
                for (String permission : role.getPermissions()) {
                    authorities.add(new SimpleGrantedAuthority(permission));
                }
            }
        }
        return authorities;
        <#elseif roleStrategy == "ENTITY">
        // Legacy Entity Strategy (for backward compatibility)
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (${roleEntity} role : this.roles) {
            String roleName = role.getName().toUpperCase();
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
            authorities.add(new SimpleGrantedAuthority(roleName));
        }
        return authorities;
        <#else>
        // Fallback: Simple String Strategy
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        </#if>
    }

    @Override
    public String getPassword() {
        return ${passwordField};
    }

    @Override
    public String getUsername() {
        return ${usernameField};
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // Use enabled field if exists later
    }
</#if>
}
