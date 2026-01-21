package ${packageName}.controller;

import ${packageName}.entity.${table.className};
import ${packageName}.service.${table.className}Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
<#if securityEnabled?? && securityEnabled>
import org.springframework.security.access.prepost.PreAuthorize;
</#if>

import java.util.List;

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

@RestController
@RequestMapping("/api/${table.className?lower_case}s")
public class ${table.className}Controller {

    private final ${table.className}Service service;

    public ${table.className}Controller(${table.className}Service service) {
    this.service = service;
    }

<#if securityEnabled?? && securityEnabled>
    <#assign entityUpper = table.className?upper_case>
    <@findSecurityRule httpMethod="GET"/>
    <#assign readRule = ruleResult>
    <@preAuthorize ruleValue=readRule defaultPermission="${entityUpper}_READ"/>
</#if>
    @GetMapping
    public List<${table.className}> getAll() {
        return service.findAll();
    }

<#if securityEnabled?? && securityEnabled>
    <@preAuthorize ruleValue=readRule defaultPermission="${entityUpper}_READ"/>
</#if>
    @GetMapping("/{id}")
    public ResponseEntity<${table.className}> getById(@PathVariable <#list table.columns as col><#if col.primaryKey>${col.javaType}</#if></#list> id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

<#if securityEnabled?? && securityEnabled>
    <@findSecurityRule httpMethod="POST"/>
    <#assign writeRule = ruleResult>
    <@preAuthorize ruleValue=writeRule defaultPermission="${entityUpper}_WRITE"/>
</#if>
    @PostMapping
    public ${table.className} create(@RequestBody ${table.className} entity) {
        return service.save(entity);
    }

<#if securityEnabled?? && securityEnabled>
    <@findSecurityRule httpMethod="DELETE"/>
    <#assign deleteRule = ruleResult>
    <@preAuthorize ruleValue=deleteRule defaultPermission="${entityUpper}_DELETE"/>
</#if>
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable <#list table.columns as col><#if col.primaryKey>${col.javaType}</#if></#list> id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
