package ${packageName}

import ${dtoPackage}.${table.className}Dto
import ${servicePackage}.${table.className}Service
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
<#if securityEnabled?? && securityEnabled && (useAnnotations!false)>
import org.springframework.security.access.prepost.PreAuthorize
</#if>

<#--
  Macro to find security rule for a given HTTP method.
  Sets the ruleResult variable in the calling scope.
-->
<#macro findSecurityRule httpMethod>
<#assign ruleResult = "">
<#if securityRules??>
    <#list securityRules as rule>
        <#if rule.method == httpMethod || rule.method == "ALL">
            <#if rule.rule == "PERMIT_ALL">
                <#assign ruleResult = "permitAll">
            <#elseif rule.rule == "AUTHENTICATED">
                <#assign ruleResult = "authenticated">
            <#elseif rule.rule == "HAS_ROLE" && rule.role??>
                <#assign ruleResult = "hasRole_" + rule.role>
            </#if>
        </#if>
    </#list>
</#if>
</#macro>

<#--
  Macro to output @PreAuthorize annotation based on security rule.
  Parameters: ruleValue - the security rule, defaultPermission - fallback permission
-->
<#macro preAuthorize ruleValue defaultPermission>
<#if ruleValue == "authenticated">
    @PreAuthorize("isAuthenticated()")
<#elseif ruleValue?starts_with("hasRole_")>
    @PreAuthorize("hasRole('${ruleValue?replace('hasRole_', '')}')")
<#elseif ruleValue != "permitAll">
    @PreAuthorize("hasAuthority('${defaultPermission}')")
</#if>
</#macro>

<#assign pkType = "Long">
<#list table.columns as col>
<#if col.primaryKey>
<#assign pkType = col.javaType>
</#if>
</#list>
<#assign resourceName = table.className?lower_case>
<#if !resourceName?ends_with("s")>
<#assign resourceName = resourceName + "s">
</#if>
@RestController
@RequestMapping("/api/${resourceName}")
class ${table.className}Controller(
    private val service: ${table.className}Service
) {

<#if securityEnabled?? && securityEnabled && (useAnnotations!false)>
    <#assign entityUpper = table.className?upper_case>
    <@findSecurityRule httpMethod="GET"/>
    <#assign readRule = ruleResult>
    <@preAuthorize ruleValue=readRule defaultPermission="${entityUpper}_READ"/>
</#if>
    @GetMapping
    fun getAll(@PageableDefault(size = 20) pageable: Pageable): Page<${table.className}Dto> =
        service.findAll(pageable)

<#if securityEnabled?? && securityEnabled && (useAnnotations!false)>
    <@preAuthorize ruleValue=readRule defaultPermission="${entityUpper}_READ"/>
</#if>
    @GetMapping("/{id}")
    fun getById(@PathVariable id: ${pkType}): ResponseEntity<${table.className}Dto> =
        service.findById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

<#if securityEnabled?? && securityEnabled && (useAnnotations!false)>
    <@findSecurityRule httpMethod="POST"/>
    <#assign writeRule = ruleResult>
    <@preAuthorize ruleValue=writeRule defaultPermission="${entityUpper}_WRITE"/>
</#if>
    @PostMapping
    fun create(@Valid @RequestBody dto: ${table.className}Dto): ResponseEntity<${table.className}Dto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto))

<#if securityEnabled?? && securityEnabled && (useAnnotations!false)>
    <@findSecurityRule httpMethod="PUT"/>
    <#assign updateRule = ruleResult>
    <@preAuthorize ruleValue=updateRule defaultPermission="${entityUpper}_WRITE"/>
</#if>
    @PutMapping("/{id}")
    fun update(@PathVariable id: ${pkType}, @Valid @RequestBody dto: ${table.className}Dto): ResponseEntity<${table.className}Dto> =
        service.update(id, dto)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

<#if securityEnabled?? && securityEnabled && (useAnnotations!false)>
    <@findSecurityRule httpMethod="DELETE"/>
    <#assign deleteRule = ruleResult>
    <@preAuthorize ruleValue=deleteRule defaultPermission="${entityUpper}_DELETE"/>
</#if>
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: ${pkType}): ResponseEntity<Void> {
        service.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
