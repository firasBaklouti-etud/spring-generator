package ${packageName}.controller;

import ${packageName}.entity.${table.className};
import ${packageName}.service.${table.className}Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
<#list table.columns as column>
<#if column.primaryKey>
import static org.mockito.ArgumentMatchers.eq;
<#break>
</#if>
</#list>
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link ${table.className}Controller}
 */
@WebMvcTest(${table.className}Controller.class)
class ${table.className}ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ${table.className}Service service;

    @Autowired
    private ObjectMapper objectMapper;

    private ${table.className} ${table.className?uncap_first};

    @BeforeEach
    void setUp() {
        ${table.className?uncap_first} = new ${table.className}();
        <#list table.columns as column>
        <#if column.primaryKey>
        ${table.className?uncap_first}.set${column.fieldName?cap_first}(1L);
        </#if>
        </#list>
        <#list table.columns as column>
        <#if !column.primaryKey && !column.autoIncrement>
        // ${table.className?uncap_first}.set${column.fieldName?cap_first}(/* TODO: set test value */);
        </#if>
        </#list>
    }

    @Test
    @DisplayName("GET /api/${table.className?lower_case} - Should return all ${table.className} entities")
    void shouldGetAll${table.className}s() throws Exception {
        List<${table.className}> list = Arrays.asList(${table.className?uncap_first});
        when(service.findAll()).thenReturn(list);

        mockMvc.perform(get("/api/${table.className?lower_case}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/${table.className?lower_case}/{id} - Should return ${table.className} by ID")
    void shouldGet${table.className}ById() throws Exception {
        <#list table.columns as column>
        <#if column.primaryKey>
        when(service.findById(1L)).thenReturn(Optional.of(${table.className?uncap_first}));

        mockMvc.perform(get("/api/${table.className?lower_case}/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.${column.fieldName}").value(1L));
        <#break>
        </#if>
        </#list>
    }

    @Test
    @DisplayName("GET /api/${table.className?lower_case}/{id} - Should return 404 when not found")
    void shouldReturn404WhenNotFound() throws Exception {
        when(service.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/${table.className?lower_case}/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/${table.className?lower_case} - Should create new ${table.className}")
    void shouldCreate${table.className}() throws Exception {
        when(service.save(any(${table.className}.class))).thenReturn(${table.className?uncap_first});

        mockMvc.perform(post("/api/${table.className?lower_case}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(${table.className?uncap_first})))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("PUT /api/${table.className?lower_case}/{id} - Should update ${table.className}")
    void shouldUpdate${table.className}() throws Exception {
        <#list table.columns as column>
        <#if column.primaryKey>
        when(service.findById(1L)).thenReturn(Optional.of(${table.className?uncap_first}));
        when(service.save(any(${table.className}.class))).thenReturn(${table.className?uncap_first});

        mockMvc.perform(put("/api/${table.className?lower_case}/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(${table.className?uncap_first})))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        <#break>
        </#if>
        </#list>
    }

    @Test
    @DisplayName("DELETE /api/${table.className?lower_case}/{id} - Should delete ${table.className}")
    void shouldDelete${table.className}() throws Exception {
        <#list table.columns as column>
        <#if column.primaryKey>
        when(service.findById(1L)).thenReturn(Optional.of(${table.className?uncap_first}));
        doNothing().when(service).deleteById(1L);

        mockMvc.perform(delete("/api/${table.className?lower_case}/1"))
                .andExpect(status().isNoContent());
        <#break>
        </#if>
        </#list>
    }
}
