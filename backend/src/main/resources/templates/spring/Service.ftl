package ${packageName}.service;

import ${packageName}.entity.${table.className};
import ${packageName}.repository.${table.className}Repository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ${table.className}Service {

    private final ${table.className}Repository repository;

    public ${table.className}Service(${table.className}Repository repository) {
    this.repository = repository;
    }

    public List<${table.className}> findAll() {
        return repository.findAll();
    }

    public Optional<${table.className}> findById(<#list table.columns as col><#if col.primaryKey>${col.javaType}</#if></#list> id) {
        return repository.findById(id);
    }

    public ${table.className} save(${table.className} entity) {
        return repository.save(entity);
    }

    public void deleteById(<#list table.columns as col><#if col.primaryKey>${col.javaType}</#if></#list> id) {
        repository.deleteById(id);
    }
}
