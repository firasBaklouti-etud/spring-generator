package ${packageName};

import ${dtoPackage}.${table.className}Dto;
import ${servicePackage}.${table.className}Service;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
<#if securityEnabled?? && securityEnabled>
import org.springframework.security.access.prepost.PreAuthorize;
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
    public Page<${table.className}Dto> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(pageable);
    }

<#if securityEnabled?? && securityEnabled>
    <@preAuthorize ruleValue=readRule defaultPermission="${entityUpper}_READ"/>
</#if>
    @GetMapping("/{id}")
    public ResponseEntity<${table.className}Dto> getById(@PathVariable ${pkType} id) {
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
    public ResponseEntity<${table.className}Dto> create(@Valid @RequestBody ${table.className}Dto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

<#if securityEnabled?? && securityEnabled>
    <@findSecurityRule httpMethod="PUT"/>
    <#assign updateRule = ruleResult>
    <@preAuthorize ruleValue=updateRule defaultPermission="${entityUpper}_WRITE"/>
</#if>
    @PutMapping("/{id}")
    public ResponseEntity<${table.className}Dto> update(@PathVariable ${pkType} id, @Valid @RequestBody ${table.className}Dto dto) {
        return service.update(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

<#if securityEnabled?? && securityEnabled>
    <@findSecurityRule httpMethod="DELETE"/>
    <#assign deleteRule = ruleResult>
    <@preAuthorize ruleValue=deleteRule defaultPermission="${entityUpper}_DELETE"/>
</#if>
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable ${pkType} id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
