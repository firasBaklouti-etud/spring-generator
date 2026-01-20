package ${packageName}.controller;

import ${packageName}.entity.${table.className};
import ${packageName}.service.${table.className}Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
<#if securityEnabled?? && securityEnabled>
import org.springframework.security.access.prepost.PreAuthorize;
</#if>

import java.util.List;

@RestController
@RequestMapping("/api/${table.className?lower_case}s")
public class ${table.className}Controller {

    private final ${table.className}Service service;

    public ${table.className}Controller(${table.className}Service service) {
    this.service = service;
    }

<#if securityEnabled?? && securityEnabled>
    <#assign entityUpper = table.className?upper_case>
    <#-- Check for READ permission rule -->
    <#assign readRule = "">
    <#if securityRules??>
        <#list securityRules as rule>
            <#if rule.method == "GET" || rule.method == "ALL">
                <#if rule.rule == "PERMIT_ALL">
                    <#assign readRule = "permitAll">
                <#elseif rule.rule == "AUTHENTICATED">
                    <#assign readRule = "authenticated">
                <#elseif rule.rule == "HAS_ROLE" && rule.role??>
                    <#assign readRule = "hasRole_" + rule.role>
                </#if>
            </#if>
        </#list>
    </#if>
    <#if readRule == "authenticated">
    @PreAuthorize("isAuthenticated()")
    <#elseif readRule?starts_with("hasRole_")>
    @PreAuthorize("hasRole('${readRule?replace('hasRole_', '')}')")
    <#elseif readRule != "permitAll">
    @PreAuthorize("hasAuthority('${entityUpper}_READ')")
    </#if>
</#if>
    @GetMapping
    public List<${table.className}> getAll() {
        return service.findAll();
    }

<#if securityEnabled?? && securityEnabled>
    <#if readRule == "authenticated">
    @PreAuthorize("isAuthenticated()")
    <#elseif readRule?starts_with("hasRole_")>
    @PreAuthorize("hasRole('${readRule?replace('hasRole_', '')}')")
    <#elseif readRule != "permitAll">
    @PreAuthorize("hasAuthority('${entityUpper}_READ')")
    </#if>
</#if>
    @GetMapping("/{id}")
    public ResponseEntity<${table.className}> getById(@PathVariable <#list table.columns as col><#if col.primaryKey>${col.javaType}</#if></#list> id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

<#if securityEnabled?? && securityEnabled>
    <#-- Check for WRITE permission rule -->
    <#assign writeRule = "">
    <#if securityRules??>
        <#list securityRules as rule>
            <#if rule.method == "POST" || rule.method == "ALL">
                <#if rule.rule == "PERMIT_ALL">
                    <#assign writeRule = "permitAll">
                <#elseif rule.rule == "AUTHENTICATED">
                    <#assign writeRule = "authenticated">
                <#elseif rule.rule == "HAS_ROLE" && rule.role??>
                    <#assign writeRule = "hasRole_" + rule.role>
                </#if>
            </#if>
        </#list>
    </#if>
    <#if writeRule == "authenticated">
    @PreAuthorize("isAuthenticated()")
    <#elseif writeRule?starts_with("hasRole_")>
    @PreAuthorize("hasRole('${writeRule?replace('hasRole_', '')}')")
    <#elseif writeRule != "permitAll">
    @PreAuthorize("hasAuthority('${entityUpper}_WRITE')")
    </#if>
</#if>
    @PostMapping
    public ${table.className} create(@RequestBody ${table.className} entity) {
        return service.save(entity);
    }

<#if securityEnabled?? && securityEnabled>
    <#-- Check for DELETE permission rule -->
    <#assign deleteRule = "">
    <#if securityRules??>
        <#list securityRules as rule>
            <#if rule.method == "DELETE" || rule.method == "ALL">
                <#if rule.rule == "PERMIT_ALL">
                    <#assign deleteRule = "permitAll">
                <#elseif rule.rule == "AUTHENTICATED">
                    <#assign deleteRule = "authenticated">
                <#elseif rule.rule == "HAS_ROLE" && rule.role??>
                    <#assign deleteRule = "hasRole_" + rule.role>
                </#if>
            </#if>
        </#list>
    </#if>
    <#if deleteRule == "authenticated">
    @PreAuthorize("isAuthenticated()")
    <#elseif deleteRule?starts_with("hasRole_")>
    @PreAuthorize("hasRole('${deleteRule?replace('hasRole_', '')}')")
    <#elseif deleteRule != "permitAll">
    @PreAuthorize("hasAuthority('${entityUpper}_DELETE')")
    </#if>
</#if>
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable <#list table.columns as col><#if col.primaryKey>${col.javaType}</#if></#list> id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
