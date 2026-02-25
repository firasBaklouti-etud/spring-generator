<#-- Map Java types to TypeScript types -->
<#function tsType javaType>
  <#if javaType == "String">
    <#return "string">
  <#elseif javaType == "Long" || javaType == "Integer" || javaType == "int" || javaType == "long" || javaType == "Double" || javaType == "Float" || javaType == "BigDecimal" || javaType == "double" || javaType == "float">
    <#return "number">
  <#elseif javaType == "Boolean" || javaType == "boolean">
    <#return "boolean">
  <#elseif javaType == "LocalDateTime" || javaType == "LocalDate" || javaType == "Date" || javaType == "Instant" || javaType == "ZonedDateTime">
    <#return "string">
  <#else>
    <#return "string">
  </#if>
</#function>
<#list tables as table>

export interface ${table.className} {
<#list table.columns as column>
  ${column.fieldName}<#if column.nullable>?</#if>: ${tsType(column.javaType)};
</#list>
}
</#list>
