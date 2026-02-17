package ${packageName};

import ${entityPackage}.${table.className};
import ${repositoryPackage}.${table.className}Repository;
import ${dtoPackage}.${table.className}Dto;
import ${mapperPackage}.${table.className}Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

<#assign pkType = "Long">
<#list table.columns as col>
<#if col.primaryKey>
<#assign pkType = col.javaType>
</#if>
</#list>
@Service
public class ${table.className}Service {

    private final ${table.className}Repository repository;

    public ${table.className}Service(${table.className}Repository repository) {
        this.repository = repository;
    }

    public List<${table.className}Dto> findAll() {
        return repository.findAll().stream()
                .map(${table.className}Mapper::toDto)
                .toList();
    }

    public Page<${table.className}Dto> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(${table.className}Mapper::toDto);
    }

    public Optional<${table.className}Dto> findById(${pkType} id) {
        return repository.findById(id)
                .map(${table.className}Mapper::toDto);
    }

    public ${table.className}Dto save(${table.className}Dto dto) {
        ${table.className} entity = ${table.className}Mapper.toEntity(dto);
        return ${table.className}Mapper.toDto(repository.save(entity));
    }

    public Optional<${table.className}Dto> update(${pkType} id, ${table.className}Dto dto) {
        return repository.findById(id)
                .map(existing -> {
                    ${table.className}Mapper.updateEntity(existing, dto);
                    return ${table.className}Mapper.toDto(repository.save(existing));
                });
    }

    public void deleteById(${pkType} id) {
        repository.deleteById(id);
    }
}
