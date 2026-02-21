package ${packageName}.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

/**
 * Request object for user registration
 */
public class RegisterRequest {

    private String username;
    private String password;
<#if security.usernameField?? && security.usernameField != "email">
    private String email;
</#if>
<#-- Add extra columns from principal entity (skip PK, FK, username, password, and role fields) -->
<#if principalTable?? && principalTable.columns??>
<#list principalTable.columns as column>
<#if !column.primaryKey
    && !column.foreignKey
    && !column.autoIncrement
    && column.fieldName != security.usernameField
    && column.fieldName != security.passwordField
    && column.fieldName != "username"
    && column.fieldName != "email"
    && column.fieldName != "roles"
    && column.fieldName != "role">
    private ${column.javaType} ${column.fieldName};
</#if>
</#list>
</#if>

    public RegisterRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

<#if security.usernameField?? && security.usernameField != "email">
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
</#if>
<#-- Getters/setters for extra columns -->
<#if principalTable?? && principalTable.columns??>
<#list principalTable.columns as column>
<#if !column.primaryKey
    && !column.foreignKey
    && !column.autoIncrement
    && column.fieldName != security.usernameField
    && column.fieldName != security.passwordField
    && column.fieldName != "username"
    && column.fieldName != "email"
    && column.fieldName != "roles"
    && column.fieldName != "role">

    public ${column.javaType} get${column.fieldName?cap_first}() {
        return ${column.fieldName};
    }

    public void set${column.fieldName?cap_first}(${column.javaType} ${column.fieldName}) {
        this.${column.fieldName} = ${column.fieldName};
    }
</#if>
</#list>
</#if>
}
