package ${packageName}

import ${entityPackage}.${table.className}
import ${repositoryPackage}.${table.className}Repository
import ${dtoPackage}.${table.className}Dto
import ${mapperPackage}.${table.className}Mapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

<#assign pkType = "Long">
<#list table.columns as col>
<#if col.primaryKey>
<#assign pkType = col.javaType>
</#if>
</#list>
@Service
class ${table.className}Service(
    private val repository: ${table.className}Repository
) {

    fun findAll(): List<${table.className}Dto> =
        repository.findAll().map { ${table.className}Mapper.toDto(it) }

    fun findAll(pageable: Pageable): Page<${table.className}Dto> =
        repository.findAll(pageable).map { ${table.className}Mapper.toDto(it) }

    fun findById(id: ${pkType}): ${table.className}Dto? =
        repository.findById(id)
            .map { ${table.className}Mapper.toDto(it) }
            .orElse(null)

    fun save(dto: ${table.className}Dto): ${table.className}Dto {
        val entity = ${table.className}Mapper.toEntity(dto)
        return ${table.className}Mapper.toDto(repository.save(entity))
    }

    fun update(id: ${pkType}, dto: ${table.className}Dto): ${table.className}Dto? =
        repository.findById(id)
            .map { existing ->
                ${table.className}Mapper.updateEntity(existing, dto)
                ${table.className}Mapper.toDto(repository.save(existing))
            }
            .orElse(null)

    fun deleteById(id: ${pkType}) {
        repository.deleteById(id)
    }
}
