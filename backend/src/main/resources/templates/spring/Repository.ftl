package ${packageName};

import ${entityPackage}.${table.className};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
<#if isUserDetails?? && isUserDetails && usernameField??>
import java.util.Optional;
</#if>

<#assign pkType = "Long">
<#list table.columns as col>
<#if col.primaryKey>
<#assign pkType = col.javaType>
</#if>
</#list>
@Repository
public interface ${table.className}Repository extends JpaRepository<${table.className}, ${pkType}> {
<#if isUserDetails?? && isUserDetails && usernameField??>
    Optional<${table.className}> findBy${usernameField?cap_first}(String ${usernameField});
</#if>
}
