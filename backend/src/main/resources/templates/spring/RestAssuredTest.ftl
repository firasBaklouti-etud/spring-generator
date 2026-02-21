package ${packageName};

import ${dtoPackage}.${table.className}Dto;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

<#-- Macro to generate a sample value based on Java type -->
<#macro testValue type fieldName>
<#if type == "String">"test${fieldName?cap_first}"<#elseif type == "Long">1L<#elseif type == "Integer" || type == "int">1<#elseif type == "Double" || type == "double">1.0<#elseif type == "Float" || type == "float">1.0f<#elseif type == "Boolean" || type == "boolean">true<#elseif type == "Short" || type == "short">(short) 1<#elseif type == "Byte" || type == "byte">(byte) 1<#elseif type == "BigDecimal">java.math.BigDecimal.valueOf(100)<#elseif type == "LocalDate">java.time.LocalDate.of(2025, 1, 1)<#elseif type == "LocalDateTime">java.time.LocalDateTime.of(2025, 1, 1, 12, 0)<#else>null</#if></#macro>
<#-- Macro to generate a JSON sample value based on Java type -->
<#macro jsonValue type fieldName>
<#if type == "String">"test${fieldName?cap_first}"<#elseif type == "Long" || type == "Integer" || type == "int" || type == "Short" || type == "short" || type == "Byte" || type == "byte">1<#elseif type == "Double" || type == "double" || type == "Float" || type == "float" || type == "BigDecimal">1.0<#elseif type == "Boolean" || type == "boolean">true<#elseif type == "LocalDate">"2025-01-01"<#elseif type == "LocalDateTime">"2025-01-01T12:00:00"<#else>null</#if></#macro>
<#assign pkType = "Long">
<#assign pkFieldName = "id">
<#list table.columns as col>
<#if col.primaryKey>
<#assign pkType = col.javaType>
<#assign pkFieldName = col.fieldName>
</#if>
</#list>
<#assign pkTestValue><@testValue type=pkType fieldName=pkFieldName/></#assign>
<#assign pkPathValue = pkTestValue?trim?replace('L', '')?replace('"', '')>
<#assign resourceName = table.className?lower_case>
<#if !resourceName?ends_with("s")>
<#assign resourceName = resourceName + "s">
</#if>
/**
 * Integration tests for {@link ${table.className}Controller} using Rest-Assured.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ${table.className}RestAssuredTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("GET /api/${resourceName} - Should return all ${table.className} entities")
    void testGetAll() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/${resourceName}")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("GET /api/${resourceName}/{id} - Should return ${table.className} by ID")
    void testGetById() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/${resourceName}/${pkPathValue}")
        .then()
            .statusCode(anyOf(is(200), is(404)))
            .contentType(anyOf(is(ContentType.JSON.toString()), nullValue()));
    }

    @Test
    @DisplayName("POST /api/${resourceName} - Should create new ${table.className}")
    void testCreate() {
        String requestBody = "{"
<#assign first = true>
<#list table.columns as column>
<#if !column.primaryKey && !column.autoIncrement && !column.foreignKey>
<#if !(isUserDetails?? && isUserDetails && passwordField?? && column.fieldName == passwordField)>
<#if first>
            + "\"${column.fieldName}\": <@jsonValue type=column.javaType fieldName=column.fieldName/>"
<#assign first = false>
<#else>
            + ", \"${column.fieldName}\": <@jsonValue type=column.javaType fieldName=column.fieldName/>"
</#if>
</#if>
</#if>
</#list>
            + "}";

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/${resourceName}")
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("PUT /api/${resourceName}/{id} - Should update ${table.className}")
    void testUpdate() {
        String requestBody = "{"
<#assign first = true>
<#list table.columns as column>
<#if !column.primaryKey && !column.autoIncrement && !column.foreignKey>
<#if !(isUserDetails?? && isUserDetails && passwordField?? && column.fieldName == passwordField)>
<#if first>
            + "\"${column.fieldName}\": <@jsonValue type=column.javaType fieldName=column.fieldName/>"
<#assign first = false>
<#else>
            + ", \"${column.fieldName}\": <@jsonValue type=column.javaType fieldName=column.fieldName/>"
</#if>
</#if>
</#if>
</#list>
            + "}";

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .put("/api/${resourceName}/${pkPathValue}")
        .then()
            .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @DisplayName("DELETE /api/${resourceName}/{id} - Should delete ${table.className}")
    void testDelete() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .delete("/api/${resourceName}/${pkPathValue}")
        .then()
            .statusCode(anyOf(is(204), is(404)));
    }
}
