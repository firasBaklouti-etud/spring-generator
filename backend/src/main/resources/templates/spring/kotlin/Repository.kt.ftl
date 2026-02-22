package ${packageName}

import ${entityPackage}.${table.className}
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

<#assign pkType = "Long">
<#list table.columns as col>
<#if col.primaryKey>
<#assign pkType = col.javaType>
</#if>
</#list>
@Repository
interface ${table.className}Repository : JpaRepository<${table.className}, ${pkType}> {
<#if isUserDetails?? && isUserDetails && usernameField??>
    fun findBy${usernameField?cap_first}(${usernameField}: String): ${table.className}?
</#if>
<#if passwordResetTokenField?? && (!isUserDetails?? || !isUserDetails || usernameField != passwordResetTokenField)>
    fun findBy${passwordResetTokenField?cap_first}(token: String): ${table.className}?
</#if>
<#list table.columns as col>
<#if col.unique && (!isUserDetails?? || !isUserDetails || usernameField != col.fieldName) && (!passwordResetTokenField?? || passwordResetTokenField != col.fieldName)>
    fun findBy${col.fieldName?cap_first}(${col.fieldName}: ${col.javaType}): ${table.className}?
</#if>
</#list>
}
