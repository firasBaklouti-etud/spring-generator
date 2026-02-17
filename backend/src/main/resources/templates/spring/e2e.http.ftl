### Generated E2E Tests for ${request.name}
@host = http://localhost:8080

<#-- Macro to check if a specific HTTP method for a resource path is PERMIT_ALL -->
<#macro checkPermitAll resourcePath httpMethod>
<#assign isPermitAll = false>
<#if request.securityConfig?? && request.securityConfig.rules??>
<#list request.securityConfig.rules as rule>
<#if (rule.path == "/api/" + resourcePath + "/**" || rule.path == "/api/" + resourcePath)>
<#if (rule.method == httpMethod || rule.method == "ALL") && rule.rule == "PERMIT_ALL">
<#assign isPermitAll = true>
</#if>
</#if>
</#list>
</#if>
</#macro>

<#if request.securityConfig?? && request.securityConfig.enabled>
### ================================================
### AUTH: Register & Login
### ================================================

### AUTH: Register
# @name register
POST {{host}}/api/auth/register
Content-Type: application/json

{
  "username": "testuser@test.com",
  "password": "Test123!",
  "email": "testuser@test.com"
}

### AUTH: Login
# @name login
POST {{host}}/api/auth/login
Content-Type: application/json

{
  "username": "testuser@test.com",
  "password": "Test123!"
}

> {%
    client.global.set("auth_token", response.body.accessToken);
    client.global.set("refresh_token", response.body.refreshToken);
%}

###
</#if>

<#if request.tables??>
### ================================================
### CRUD Tests
### ================================================
<#list request.tables as table>
<#if !table.joinTable && !(table.metadata.isRoleEntity?? && table.metadata.isRoleEntity)>
<#assign resourceName = table.className?lower_case>
<#if !resourceName?ends_with("s")>
<#assign resourceName = resourceName + "s">
</#if>

### ------------------------------------------------
### CRUD ${table.className}
### ------------------------------------------------

### List ${table.className}
GET {{host}}/api/${resourceName}
<#if request.securityConfig?? && request.securityConfig.enabled>
<@checkPermitAll resourcePath=resourceName httpMethod="GET"/>
<#if !isPermitAll>
Authorization: Bearer {{auth_token}}
</#if>
</#if>

### Create ${table.className}
# @name create${table.className}
POST {{host}}/api/${resourceName}
<#if request.securityConfig?? && request.securityConfig.enabled>
Authorization: Bearer {{auth_token}}
</#if>
Content-Type: application/json

{
<#assign first = true>
<#list table.columns as col>
<#if !col.primaryKey && !col.foreignKey>
<#-- Skip password field for security entity -->
<#if !(isUserDetails?? && isUserDetails && passwordField?? && col.fieldName == passwordField)>
<#if !first>,
</#if>
  "${col.fieldName}": <#if col.javaType == "String">"test_${col.fieldName}"<#elseif col.javaType == "Integer" || col.javaType == "Long" || col.javaType == "int">1<#elseif col.javaType == "Double" || col.javaType == "BigDecimal" || col.javaType == "java.math.BigDecimal">10.0<#elseif col.javaType == "Boolean" || col.javaType == "boolean">true<#elseif col.javaType == "LocalDateTime" || col.javaType == "java.time.LocalDateTime">"2025-01-01T12:00:00"<#elseif col.javaType == "LocalDate" || col.javaType == "java.time.LocalDate">"2025-01-01"<#else>null</#if><#assign first = false>
</#if>
</#if>
</#list>

}

> {% client.global.set("${table.className?lower_case}_id", response.body.id); %}

### Get ${table.className} by ID
GET {{host}}/api/${resourceName}/{{${table.className?lower_case}_id}}
<#if request.securityConfig?? && request.securityConfig.enabled>
<@checkPermitAll resourcePath=resourceName httpMethod="GET"/>
<#if !isPermitAll>
Authorization: Bearer {{auth_token}}
</#if>
</#if>

###
</#if>
</#list>
</#if>

<#if request.securityConfig?? && request.securityConfig.enabled>
<#if request.securityConfig.rbacMode?? && request.securityConfig.rbacMode == "DYNAMIC">
### ================================================
### ADMIN API: Role Management (requires ADMIN role)
### ================================================

### Admin Login (seeded by DataInitializer)
# @name adminLogin
POST {{host}}/api/auth/login
Content-Type: application/json

{
  "username": "admin@admin.com",
  "password": "admin123"
}

> {%
    client.global.set("admin_token", response.body.accessToken);
%}

### List All Roles
GET {{host}}/api/admin/roles
Authorization: Bearer {{admin_token}}

### Create New Role
# @name createRole
POST {{host}}/api/admin/roles
Authorization: Bearer {{admin_token}}
Content-Type: application/json

{
  "name": "MODERATOR",
  "description": "Moderator role with limited admin access",
  "permissions": ["USER_READ", "PRODUCT_READ"]
}

> {% client.global.set("new_role_id", response.body.id); %}

### Update Role
PUT {{host}}/api/admin/roles/{{new_role_id}}
Authorization: Bearer {{admin_token}}
Content-Type: application/json

{
  "name": "MODERATOR",
  "description": "Updated moderator role",
  "permissions": ["USER_READ", "PRODUCT_READ", "ORDER_READ"]
}

### Get User Roles
GET {{host}}/api/admin/users/1/roles
Authorization: Bearer {{admin_token}}

### Delete Role
DELETE {{host}}/api/admin/roles/{{new_role_id}}
Authorization: Bearer {{admin_token}}

###
</#if>

### ================================================
### AUTH: Token Refresh
### ================================================

### Refresh Token
POST {{host}}/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "{{refresh_token}}"
}

###
</#if>
