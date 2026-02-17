package ${packageName};

import ${entityPackage}.${table.className};
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

<#-- Macro to generate a sample value based on Java type -->
<#macro testValue type fieldName>
<#if type == "String">"test${fieldName?cap_first}"<#elseif type == "Long">1L<#elseif type == "Integer" || type == "int">1<#elseif type == "Double" || type == "double">1.0<#elseif type == "Float" || type == "float">1.0f<#elseif type == "Boolean" || type == "boolean">true<#elseif type == "Short" || type == "short">(short) 1<#elseif type == "Byte" || type == "byte">(byte) 1<#elseif type == "BigDecimal">java.math.BigDecimal.valueOf(100)<#elseif type == "LocalDate">java.time.LocalDate.of(2025, 1, 1)<#elseif type == "LocalDateTime">java.time.LocalDateTime.of(2025, 1, 1, 12, 0)<#else>null</#if></#macro>
<#-- Macro to generate a second distinct sample value for update tests -->
<#macro testValue2 type fieldName>
<#if type == "String">"updated${fieldName?cap_first}"<#elseif type == "Long">2L<#elseif type == "Integer" || type == "int">2<#elseif type == "Double" || type == "double">2.0<#elseif type == "Float" || type == "float">2.0f<#elseif type == "Boolean" || type == "boolean">false<#elseif type == "Short" || type == "short">(short) 2<#elseif type == "Byte" || type == "byte">(byte) 2<#elseif type == "BigDecimal">java.math.BigDecimal.valueOf(200)<#elseif type == "LocalDate">java.time.LocalDate.of(2025, 6, 15)<#elseif type == "LocalDateTime">java.time.LocalDateTime.of(2025, 6, 15, 14, 30)<#else>null</#if></#macro>
<#assign pkFieldName = "id">
<#list table.columns as col>
<#if col.primaryKey>
<#assign pkFieldName = col.fieldName>
</#if>
</#list>
<#-- Find the first non-PK, non-autoincrement column for update testing -->
<#assign updateColumn = "">
<#list table.columns as column>
<#if !column.primaryKey && !column.autoIncrement && !column.foreignKey>
<#assign updateColumn = column>
<#break>
</#if>
</#list>
/**
 * Integration tests for {@link ${table.className}Repository}
 */
@DataJpaTest
class ${table.className}RepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ${table.className}Repository repository;

    private ${table.className} ${table.className?uncap_first};

    @BeforeEach
    void setUp() {
        ${table.className?uncap_first} = new ${table.className}();
<#list table.columns as column>
<#if !column.primaryKey && !column.autoIncrement && !column.foreignKey>
        ${table.className?uncap_first}.set${column.fieldName?cap_first}(<@testValue type=column.javaType fieldName=column.fieldName/>);
</#if>
</#list>
        ${table.className?uncap_first} = entityManager.persistAndFlush(${table.className?uncap_first});
    }

    @Test
    @DisplayName("Should save ${table.className} successfully")
    void shouldSave${table.className}() {
        ${table.className} new${table.className} = new ${table.className}();
<#list table.columns as column>
<#if !column.primaryKey && !column.autoIncrement && !column.foreignKey>
        new${table.className}.set${column.fieldName?cap_first}(<@testValue type=column.javaType fieldName=column.fieldName/>);
</#if>
</#list>

        ${table.className} saved = repository.save(new${table.className});

        assertThat(saved).isNotNull();
        assertThat(saved.get${pkFieldName?cap_first}()).isNotNull();
    }

    @Test
    @DisplayName("Should find ${table.className} by ID")
    void shouldFind${table.className}ById() {
        Optional<${table.className}> found = repository.findById(${table.className?uncap_first}.get${pkFieldName?cap_first}());

        assertThat(found).isPresent();
        assertThat(found.get().get${pkFieldName?cap_first}()).isEqualTo(${table.className?uncap_first}.get${pkFieldName?cap_first}());
    }

    @Test
    @DisplayName("Should find all ${table.className} entities")
    void shouldFindAll${table.className}s() {
        List<${table.className}> all = repository.findAll();

        assertThat(all).isNotEmpty();
        assertThat(all).contains(${table.className?uncap_first});
    }

    @Test
    @DisplayName("Should update ${table.className} successfully")
    void shouldUpdate${table.className}() {
<#if updateColumn?has_content>
        ${table.className?uncap_first}.set${updateColumn.fieldName?cap_first}(<@testValue2 type=updateColumn.javaType fieldName=updateColumn.fieldName/>);
</#if>

        ${table.className} updated = repository.save(${table.className?uncap_first});
        entityManager.flush();
        entityManager.clear();

        Optional<${table.className}> found = repository.findById(updated.get${pkFieldName?cap_first}());
        assertThat(found).isPresent();
<#if updateColumn?has_content>
        assertThat(found.get().get${updateColumn.fieldName?cap_first}()).isEqualTo(<@testValue2 type=updateColumn.javaType fieldName=updateColumn.fieldName/>);
</#if>
    }

    @Test
    @DisplayName("Should delete ${table.className} successfully")
    void shouldDelete${table.className}() {
        repository.deleteById(${table.className?uncap_first}.get${pkFieldName?cap_first}());
        entityManager.flush();

        Optional<${table.className}> deleted = repository.findById(${table.className?uncap_first}.get${pkFieldName?cap_first}());
        assertThat(deleted).isEmpty();
    }
}
