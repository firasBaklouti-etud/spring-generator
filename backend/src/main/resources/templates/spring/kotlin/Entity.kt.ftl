package ${packageName}

import jakarta.persistence.*
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
import java.time.LocalDate
</#if>
<#if hasDateTime>
import java.time.LocalDateTime
</#if>
<#if hasBigDecimal>
import java.math.BigDecimal
</#if>
<#if hasUuid>
import java.util.UUID
</#if>

<#if isUserDetails?? && isUserDetails>
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import com.fasterxml.jackson.annotation.JsonIgnore
<#if rbacMode?? && rbacMode == "STATIC">
import ${basePackageName}.security.Role
</#if>
</#if>

@Entity
@Table(name = "${table.name}")
class ${table.className}<#if isUserDetails?? && isUserDetails> : UserDetails</#if> {

<#list table.columns as column>
    <#if !column.foreignKey>
    <#if column.primaryKey>
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    </#if>
    <#if isUserDetails?? && isUserDetails && column.fieldName == passwordField>
    @JsonIgnore
    </#if>
    @Column(name = "${column.name}")
    var ${column.fieldName}: ${column.javaType}<#if column.nullable>?</#if> = <#if column.primaryKey>null<#elseif column.nullable>null<#elseif column.javaType == "String">""<#elseif column.javaType == "Int" || column.javaType == "Integer">0<#elseif column.javaType == "Long">0L<#elseif column.javaType == "Double">0.0<#elseif column.javaType == "Float">0.0f<#elseif column.javaType == "Boolean">false<#elseif column.javaType == "BigDecimal">BigDecimal.ZERO<#else>null</#if>

    </#if>
</#list>

<#if hasRelationships>
<#list table.relationships as rel>
    <#if rel.type == "MANY_TO_ONE">
    @ManyToOne
    @JoinColumn(name = "${rel.sourceColumn}")
    var ${rel.fieldName}: ${rel.targetClassName}? = null

    <#elseif rel.type == "ONE_TO_MANY">
    @OneToMany(mappedBy = "${rel.mappedBy}")
    var ${rel.fieldName}: MutableList<${rel.targetClassName}> = mutableListOf()

    <#elseif rel.type == "ONE_TO_ONE">
    <#if rel.sourceColumn??>
    @OneToOne
    @JoinColumn(name = "${rel.sourceColumn}")
    <#else>
    @OneToOne(mappedBy = "${rel.mappedBy}")
    </#if>
    var ${rel.fieldName}: ${rel.targetClassName}? = null

    <#elseif rel.type == "MANY_TO_MANY">
    <#assign isRolesRelation = (isUserDetails?? && isUserDetails && rel.fieldName == "roles")>
    @ManyToMany(fetch = FetchType.<#if isRolesRelation>EAGER<#else>LAZY</#if>)
    @JoinTable(
        name = "${rel.joinTable}",
        joinColumns = [JoinColumn(name = "${rel.sourceColumn}")],
        inverseJoinColumns = [JoinColumn(name = "${rel.targetColumn}")]
    )
    var ${rel.fieldName}: MutableList<${rel.targetClassName}> = mutableListOf()

    </#if>
</#list>
</#if>

<#if isUserDetails?? && isUserDetails>
    override fun getAuthorities(): Collection<GrantedAuthority> {
        <#if rbacMode?? && rbacMode == "STATIC">
        // Static RBAC Mode: Use Role Enum
        <#if roleField??>
        return this.${roleField}.getAuthorities()
            .map { SimpleGrantedAuthority(it) }
        <#else>
        return listOf(SimpleGrantedAuthority("ROLE_USER"))
        </#if>
        <#elseif rbacMode?? && rbacMode == "DYNAMIC">
        // Dynamic RBAC Mode: Use Role Entity with ManyToMany relationship
        val authorities = mutableListOf<SimpleGrantedAuthority>()
        for (role in this.roles) {
            var roleName = role.name.uppercase()
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_$roleName"
            }
            authorities.add(SimpleGrantedAuthority(roleName))

            // Add permissions if role has them
            role.permissions?.forEach { permission ->
                authorities.add(SimpleGrantedAuthority(permission))
            }
        }
        return authorities
        <#elseif roleStrategy == "ENTITY">
        // Legacy Entity Strategy (for backward compatibility)
        val authorities = mutableListOf<SimpleGrantedAuthority>()
        for (role in this.roles) {
            var roleName = role.name.uppercase()
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_$roleName"
            }
            authorities.add(SimpleGrantedAuthority(roleName))
        }
        return authorities
        <#else>
        // Fallback: Simple String Strategy
        return listOf(SimpleGrantedAuthority("ROLE_USER"))
        </#if>
    }

    override fun getPassword(): String {
        return ${passwordField}
    }

    override fun getUsername(): String {
        return ${usernameField}
    }

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
</#if>
}
