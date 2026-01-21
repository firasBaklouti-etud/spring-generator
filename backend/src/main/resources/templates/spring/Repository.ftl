package ${packageName}.repository;

import ${packageName}.entity.${table.className};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ${table.className}Repository extends JpaRepository<${table.className}, <#list table.columns as col><#if col.primaryKey>${col.javaType}</#if></#list>> {
}
