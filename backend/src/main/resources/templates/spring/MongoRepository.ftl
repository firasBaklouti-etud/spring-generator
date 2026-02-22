package ${packageName};

import ${entityPackage}.${table.className};
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ${table.className}Repository extends MongoRepository<${table.className}, String> {
<#if isUserDetails?? && isUserDetails && usernameField??>
    Optional<${table.className}> findBy${usernameField?cap_first}(String ${usernameField});
</#if>
<#if passwordResetTokenField?? && (!isUserDetails?? || !isUserDetails || usernameField != passwordResetTokenField)>
    Optional<${table.className}> findBy${passwordResetTokenField?cap_first}(String token);
</#if>
<#list table.columns as col>
<#if col.unique && (!isUserDetails?? || !isUserDetails || usernameField != col.fieldName) && (!passwordResetTokenField?? || passwordResetTokenField != col.fieldName)>
    Optional<${table.className}> findBy${col.fieldName?cap_first}(${col.javaType} ${col.fieldName});
</#if>
</#list>
}
