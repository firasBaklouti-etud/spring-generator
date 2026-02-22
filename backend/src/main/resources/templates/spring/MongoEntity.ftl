package ${packageName};

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.index.Indexed;
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
<#assign hasDate = false>
<#assign hasDateTime = false>
<#assign hasBigDecimal = false>
<#assign hasUuid = false>
<#list table.columns as column>
<#if !column.foreignKey>
<#if column.javaType == "LocalDate">
<#assign hasDate = true>
</#if>
<#if column.javaType == "LocalDateTime">
<#assign hasDateTime = true>
</#if>
<#if column.javaType == "BigDecimal" || column.javaType == "java.math.BigDecimal">
<#assign hasBigDecimal = true>
</#if>
<#if column.javaType == "UUID" || column.javaType == "java.util.UUID">
<#assign hasUuid = true>
</#if>
</#if>
</#list>
<#if hasDate>
import java.time.LocalDate;
</#if>
<#if hasDateTime>
import java.time.LocalDateTime;
</#if>
<#if hasBigDecimal>
import java.math.BigDecimal;
</#if>
<#if hasUuid>
import java.util.UUID;
</#if>

<#if isUserDetails?? && isUserDetails>
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
</#if>
<#if isUserDetails?? && isUserDetails>
import com.fasterxml.jackson.annotation.JsonIgnore;
<#if rbacMode?? && rbacMode == "STATIC">
import ${basePackageName}.security.Role;
</#if>
</#if>

@Document(collection = "${table.name}")
public class ${table.className} <#if isUserDetails?? && isUserDetails>implements UserDetails</#if> {

<#list table.columns as column>
    <#if !column.foreignKey>
    <#if column.primaryKey>
    @Id
    private String id;

    <#else>
    <#if isUserDetails?? && isUserDetails && column.fieldName == passwordField>
    @JsonIgnore
    </#if>
    <#if column.unique>
    @Indexed(unique = true)
    </#if>
    @Field("${column.name}")
    private ${column.javaType} ${column.fieldName};

    </#if>
    </#if>
</#list>

<#if hasRelationships>
<#list table.relationships as rel>
    <#if rel.type == "MANY_TO_ONE">
    @DBRef
    private ${rel.targetClassName} ${rel.fieldName};

    <#elseif rel.type == "ONE_TO_MANY">
    @DBRef
    private List<${rel.targetClassName}> ${rel.fieldName} = new ArrayList<>();

    <#elseif rel.type == "ONE_TO_ONE">
    @DBRef
    private ${rel.targetClassName} ${rel.fieldName};

    <#elseif rel.type == "MANY_TO_MANY">
    @DBRef
    private List<${rel.targetClassName}> ${rel.fieldName} = new ArrayList<>();

    </#if>
</#list>
</#if>

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

<#list table.columns as column>
    <#if !column.foreignKey>
    <#if !column.primaryKey>
    <#-- Only suppress getters whose method name collides with UserDetails: getUsername(), getPassword() -->
    <#assign isUserDetailsField = isUserDetails?? && isUserDetails && ((column.fieldName == "username") || (column.fieldName == "password"))>
    <#-- If usernameField is not "username" but the column IS "username", provide an alias getter -->
    <#assign needsAliasGetter = isUserDetails?? && isUserDetails && column.fieldName == "username" && usernameField?? && usernameField != "username">
    <#if !isUserDetailsField>
    public ${column.javaType} get${column.fieldName?cap_first}() {
        return ${column.fieldName};
    }

    </#if>
    <#if needsAliasGetter>
    /**
     * Access the actual 'username' column value.
     * Note: getUsername() is overridden by UserDetails to return the login identifier (${usernameField}).
     */
    public ${column.javaType} getUsernameValue() {
        return username;
    }

    </#if>
    public void set${column.fieldName?cap_first}(${column.javaType} ${column.fieldName}) {
        this.${column.fieldName} = ${column.fieldName};
    }

    </#if>
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
        // Dynamic RBAC Mode: Use Role Entity with DBRef relationship
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
