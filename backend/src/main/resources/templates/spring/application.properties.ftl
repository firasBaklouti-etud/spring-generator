# Application Configuration
spring.application.name=${request.artifactId}

# ==================== Database Configuration ====================
<#if request.databaseType?? && request.databaseType == "postgresql">
# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/${request.artifactId?replace("-", "_")}
spring.datasource.username=postgres
spring.datasource.password=
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
<#elseif request.databaseType?? && request.databaseType == "mariadb">
# MariaDB Configuration
spring.datasource.url=jdbc:mariadb://localhost:3306/${request.artifactId?replace("-", "_")}
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
<#elseif request.databaseType?? && request.databaseType == "sqlserver">
# SQL Server Configuration
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=${request.artifactId?replace("-", "_")};encrypt=false
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect
<#elseif request.databaseType?? && request.databaseType == "h2">
# H2 In-Memory Database Configuration
spring.datasource.url=jdbc:h2:mem:${request.artifactId?replace("-", "_")};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
<#else>
# MySQL Configuration (default)
spring.datasource.url=jdbc:mysql://localhost:3306/${request.artifactId?replace("-", "_")}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
</#if>

# JPA/Hibernate Configuration
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool != "none">
spring.jpa.hibernate.ddl-auto=validate
<#else>
spring.jpa.hibernate.ddl-auto=update
</#if>
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.globally_quoted_identifiers=true

<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool == "flyway">
# ==================== Flyway Migrations ====================
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
</#if>
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool == "liquibase">
# ==================== Liquibase Migrations ====================
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
</#if>

# Server Configuration
server.port=8080

<#if request.securityConfig?? && request.securityConfig.enabled>
# ==================== Security Configuration ====================
<#if hasJwt?? && hasJwt>
# JWT Configuration
# SECURITY: Set JWT_SECRET env variable in production with a secure random key
# Generate with: openssl rand -base64 32
jwt.secret=${"$"}{JWT_SECRET:MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}
jwt.expiration=36000000
jwt.refresh-expiration=604800000
<#else>
# Basic Authentication is enabled
# Configure users in SecurityConfig.java or use database authentication
</#if>
</#if>

<#if request.includeDocker?? && request.includeDocker>
# ==================== Docker Configuration ====================
# When running in Docker, use container hostnames instead of localhost
# spring.datasource.url=jdbc:mysql://db:3306/${request.artifactId?replace("-", "_")}
</#if>
