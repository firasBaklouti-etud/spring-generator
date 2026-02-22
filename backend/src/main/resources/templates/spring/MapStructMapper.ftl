package ${packageName};

import ${entityPackage}.${table.className};
import ${dtoPackage}.${table.className}Dto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
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
import java.util.List;

@Mapper(componentModel = "spring")
public interface ${table.className}Mapper {

<#if hasRelationships>
<#list table.relationships as rel>
<#assign isRoleRel = (isUserDetails?? && isUserDetails && rel.fieldName == "roles")>
<#if !isRoleRel>
    @Mapping(target = "${rel.fieldName}", ignore = true)
</#if>
</#list>
</#if>
<#if isUserDetails?? && isUserDetails && passwordField??>
    @Mapping(target = "${passwordField}", ignore = true)
</#if>
    ${table.className}Dto toDto(${table.className} entity);

<#if hasRelationships>
<#list table.relationships as rel>
<#assign isRoleRel = (isUserDetails?? && isUserDetails && rel.fieldName == "roles")>
<#if !isRoleRel>
    @Mapping(target = "${rel.fieldName}", ignore = true)
</#if>
</#list>
</#if>
<#-- Ignore primary key when creating a new entity from DTO -->
<#list table.columns as column>
<#if column.primaryKey>
    @Mapping(target = "${column.fieldName}", ignore = true)
</#if>
</#list>
    ${table.className} toEntity(${table.className}Dto dto);

    List<${table.className}Dto> toDtoList(List<${table.className}> entities);

<#if hasRelationships>
<#list table.relationships as rel>
<#assign isRoleRel = (isUserDetails?? && isUserDetails && rel.fieldName == "roles")>
<#if !isRoleRel>
    @Mapping(target = "${rel.fieldName}", ignore = true)
</#if>
</#list>
</#if>
<#list table.columns as column>
<#if column.primaryKey>
    @Mapping(target = "${column.fieldName}", ignore = true)
</#if>
</#list>
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget ${table.className} entity, ${table.className}Dto dto);
}
