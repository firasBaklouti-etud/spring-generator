package ${packageName};

import ${entityPackage}.${table.className};
import ${dtoPackage}.${table.className}Dto;
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
import java.util.stream.Collectors;
</#if>

public class ${table.className}Mapper {

    public static ${table.className}Dto toDto(${table.className} entity) {
        if (entity == null) {
            return null;
        }
        ${table.className}Dto dto = new ${table.className}Dto();
<#list table.columns as column>
<#if !column.foreignKey>
<#-- Skip password field in DTO mapping (security-sensitive) -->
<#if !(isUserDetails?? && isUserDetails && passwordField?? && column.fieldName == passwordField)>
        dto.set${column.fieldName?cap_first}(entity.get${column.fieldName?cap_first}());
</#if>
</#if>
</#list>
        return dto;
    }

    public static ${table.className} toEntity(${table.className}Dto dto) {
        if (dto == null) {
            return null;
        }
        ${table.className} entity = new ${table.className}();
<#list table.columns as column>
<#if !column.foreignKey && !column.primaryKey>
<#if !(isUserDetails?? && isUserDetails && passwordField?? && column.fieldName == passwordField)>
        entity.set${column.fieldName?cap_first}(dto.get${column.fieldName?cap_first}());
</#if>
</#if>
</#list>
        return entity;
    }

    public static void updateEntity(${table.className} entity, ${table.className}Dto dto) {
        if (entity == null || dto == null) {
            return;
        }
<#list table.columns as column>
<#if !column.foreignKey && !column.primaryKey>
<#if !(isUserDetails?? && isUserDetails && passwordField?? && column.fieldName == passwordField)>
        entity.set${column.fieldName?cap_first}(dto.get${column.fieldName?cap_first}());
</#if>
</#if>
</#list>
    }
<#if hasCollections>

    public static List<${table.className}Dto> toDtoList(List<${table.className}> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(${table.className}Mapper::toDto)
                .collect(Collectors.toList());
    }
</#if>
}
