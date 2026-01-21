package ${packageName}.repository;

import ${packageName}.entity.${table.className};
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
        <#if !column.primaryKey && !column.autoIncrement>
        // ${table.className?uncap_first}.set${column.fieldName?cap_first}(/* TODO: set test value */);
        </#if>
        </#list>
        ${table.className?uncap_first} = entityManager.persistAndFlush(${table.className?uncap_first});
    }

    @Test
    @DisplayName("Should save ${table.className} successfully")
    void shouldSave${table.className}() {
        ${table.className} new${table.className} = new ${table.className}();
        <#list table.columns as column>
        <#if !column.primaryKey && !column.autoIncrement>
        // new${table.className}.set${column.fieldName?cap_first}(/* TODO: set test value */);
        </#if>
        </#list>

        ${table.className} saved = repository.save(new${table.className});

        assertThat(saved).isNotNull();
        <#list table.columns as column>
        <#if column.primaryKey>
        assertThat(saved.get${column.fieldName?cap_first}()).isNotNull();
        </#if>
        </#list>
    }

    @Test
    @DisplayName("Should find ${table.className} by ID")
    void shouldFind${table.className}ById() {
        <#list table.columns as column>
        <#if column.primaryKey>
        Optional<${table.className}> found = repository.findById(${table.className?uncap_first}.get${column.fieldName?cap_first}());
        </#if>
        </#list>

        assertThat(found).isPresent();
        <#list table.columns as column>
        <#if column.primaryKey>
        assertThat(found.get().get${column.fieldName?cap_first}()).isEqualTo(${table.className?uncap_first}.get${column.fieldName?cap_first}());
        </#if>
        </#list>
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
        // Modify the entity
        // ${table.className?uncap_first}.set...(/* new value */);

        ${table.className} updated = repository.save(${table.className?uncap_first});
        entityManager.flush();
        entityManager.clear();

        <#list table.columns as column>
        <#if column.primaryKey>
        Optional<${table.className}> found = repository.findById(updated.get${column.fieldName?cap_first}());
        assertThat(found).isPresent();
        </#if>
        </#list>
    }

    @Test
    @DisplayName("Should delete ${table.className} successfully")
    void shouldDelete${table.className}() {
        <#list table.columns as column>
        <#if column.primaryKey>
        repository.deleteById(${table.className?uncap_first}.get${column.fieldName?cap_first}());
        entityManager.flush();

        Optional<${table.className}> deleted = repository.findById(${table.className?uncap_first}.get${column.fieldName?cap_first}());
        assertThat(deleted).isEmpty();
        </#if>
        </#list>
    }
}
