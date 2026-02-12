package ${packageName}.dto;

<#assign hasDate = false>
<#assign hasDateTime = false>
<#assign hasBigDecimal = false>
<#list table.columns as column>
<#if !column.foreignKey>
<#if column.javaType == "LocalDate">
<#assign hasDate = true>
</#if>
<#if column.javaType == "LocalDateTime">
<#assign hasDateTime = true>
</#if>
<#if column.javaType == "java.math.BigDecimal" || column.javaType == "BigDecimal">
<#assign hasBigDecimal = true>
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
</#if>

public class ${table.className}Dto {

<#list table.columns as column>
<#if !column.foreignKey>
    private ${column.javaType} ${column.fieldName};
</#if>
</#list>
<#if hasRelationships>
<#list table.relationships as rel>
<#if rel.type == "MANY_TO_ONE" || rel.type == "ONE_TO_ONE">
    private <#list rel.targetTable?split("_") as part>${part?cap_first}</#list>Dto ${rel.fieldName};
<#elseif rel.type == "ONE_TO_MANY" || rel.type == "MANY_TO_MANY">
    private List<<#list rel.targetTable?split("_") as part>${part?cap_first}</#list>Dto> ${rel.fieldName};
</#if>
</#list>
</#if>

    public ${table.className}Dto() {
    }

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
<#if rel.type == "MANY_TO_ONE" || rel.type == "ONE_TO_ONE">
    public <#list rel.targetTable?split("_") as part>${part?cap_first}</#list>Dto get${rel.fieldName?cap_first}() {
        return ${rel.fieldName};
    }

    public void set${rel.fieldName?cap_first}(<#list rel.targetTable?split("_") as part>${part?cap_first}</#list>Dto ${rel.fieldName}) {
        this.${rel.fieldName} = ${rel.fieldName};
    }

<#elseif rel.type == "ONE_TO_MANY" || rel.type == "MANY_TO_MANY">
    public List<<#list rel.targetTable?split("_") as part>${part?cap_first}</#list>Dto> get${rel.fieldName?cap_first}() {
        return ${rel.fieldName};
    }

    public void set${rel.fieldName?cap_first}(List<<#list rel.targetTable?split("_") as part>${part?cap_first}</#list>Dto> ${rel.fieldName}) {
        this.${rel.fieldName} = ${rel.fieldName};
    }

</#if>
</#list>
</#if>
}
