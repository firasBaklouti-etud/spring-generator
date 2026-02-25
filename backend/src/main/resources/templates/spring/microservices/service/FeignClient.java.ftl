package ${packageName}.client;

<#assign targetClassName = targetTable.className>
<#-- Determine the primary key type from the target table -->
<#assign idType = "Long">
<#list targetTable.columns as col>
<#if col.primaryKey>
<#assign idType = col.javaType!"Long">
<#break>
</#if>
</#list>
import ${packageName}.client.dto.${targetClassName}Dto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@FeignClient(name = "${targetServiceName}", path = "/api/${targetTable.name?lower_case}")
public interface ${targetClassName}ServiceClient {

	@GetMapping("/{id}")
	${targetClassName}Dto findById(@PathVariable("id") ${idType} id);

	@GetMapping
	List<${targetClassName}Dto> findAll();

}
