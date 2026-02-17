package ${packageName};

import ${dtoPackage}.${table.className}Dto;
import ${servicePackage}.${table.className}Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
<#if securityEnabled?? && securityEnabled>
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
</#if>
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
<#if securityEnabled?? && securityEnabled>
import ${basePackageName}.config.JwtUtil;
import ${basePackageName}.config.JwtAuthenticationFilter;
import ${basePackageName}.service.auth.CustomUserDetailsService;
</#if>

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

<#-- Macro to generate a sample value based on Java type -->
<#macro testValue type fieldName>
<#if type == "String">"test${fieldName?cap_first}"<#elseif type == "Long">1L<#elseif type == "Integer" || type == "int">1<#elseif type == "Double" || type == "double">1.0<#elseif type == "Float" || type == "float">1.0f<#elseif type == "Boolean" || type == "boolean">true<#elseif type == "Short" || type == "short">(short) 1<#elseif type == "Byte" || type == "byte">(byte) 1<#elseif type == "BigDecimal">java.math.BigDecimal.valueOf(100)<#elseif type == "LocalDate">java.time.LocalDate.of(2025, 1, 1)<#elseif type == "LocalDateTime">java.time.LocalDateTime.of(2025, 1, 1, 12, 0)<#else>null</#if></#macro>
<#assign pkType = "Long">
<#assign pkFieldName = "id">
<#list table.columns as col>
<#if col.primaryKey>
<#assign pkType = col.javaType>
<#assign pkFieldName = col.fieldName>
</#if>
</#list>
<#assign pkTestValue><@testValue type=pkType fieldName=pkFieldName/></#assign>
<#assign resourceName = table.className?lower_case>
<#if !resourceName?ends_with("s")>
<#assign resourceName = resourceName + "s">
</#if>
/**
 * Unit tests for {@link ${table.className}Controller}
 */
@WebMvcTest(${table.className}Controller.class)
<#if securityEnabled?? && securityEnabled>
@AutoConfigureMockMvc(addFilters = false)
</#if>
class ${table.className}ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ${table.className}Service service;

<#if securityEnabled?? && securityEnabled>
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

</#if>
    @Autowired
    private ObjectMapper objectMapper;

    private ${table.className}Dto dto;

    @BeforeEach
    void setUp() {
        dto = new ${table.className}Dto();
<#list table.columns as column>
<#if column.primaryKey>
        dto.set${column.fieldName?cap_first}(${pkTestValue?trim});
<#elseif !column.autoIncrement && !column.foreignKey>
<#-- Skip password field since DTO excludes it for security entities -->
<#if !(isUserDetails?? && isUserDetails && passwordField?? && column.fieldName == passwordField)>
        dto.set${column.fieldName?cap_first}(<@testValue type=column.javaType fieldName=column.fieldName/>);
</#if>
</#if>
</#list>
    }

    @Test
    @DisplayName("GET /api/${resourceName} - Should return all ${table.className} entities")
    void shouldGetAll${table.className}s() throws Exception {
        Page<${table.className}Dto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        when(service.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/${resourceName}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/${resourceName}/{id} - Should return ${table.className} by ID")
    void shouldGet${table.className}ById() throws Exception {
        when(service.findById(${pkTestValue?trim})).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/${resourceName}/${pkTestValue?trim?replace('L', '')?replace('\"', '')}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/${resourceName}/{id} - Should return 404 when not found")
    void shouldReturn404WhenNotFound() throws Exception {
        when(service.findById(${pkTestValue?trim})).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/${resourceName}/${pkTestValue?trim?replace('L', '')?replace('\"', '')}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/${resourceName} - Should create new ${table.className}")
    void shouldCreate${table.className}() throws Exception {
        when(service.save(any(${table.className}Dto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/${resourceName}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("PUT /api/${resourceName}/{id} - Should update ${table.className}")
    void shouldUpdate${table.className}() throws Exception {
        when(service.update(eq(${pkTestValue?trim}), any(${table.className}Dto.class))).thenReturn(Optional.of(dto));

        mockMvc.perform(put("/api/${resourceName}/${pkTestValue?trim?replace('L', '')?replace('\"', '')}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("DELETE /api/${resourceName}/{id} - Should delete ${table.className}")
    void shouldDelete${table.className}() throws Exception {
        doNothing().when(service).deleteById(${pkTestValue?trim});

        mockMvc.perform(delete("/api/${resourceName}/${pkTestValue?trim?replace('L', '')?replace('\"', '')}"))
                .andExpect(status().isNoContent());
    }
}
