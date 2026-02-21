# ==================== Development Profile ====================
# Activate with: --spring.profiles.active=dev
# Uses H2 in-memory database for rapid local development

# H2 In-Memory Database
spring.datasource.url=jdbc:h2:mem:${request.artifactId?replace("-", "_")}_dev;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# H2 Console (available at /h2-console)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA/Hibernate - auto-create tables for dev
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.globally_quoted_identifiers=true
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool != "none">

# Disable migrations for dev profile (uses ddl-auto instead)
<#if springConfig.migrationTool == "flyway">
spring.flyway.enabled=false
<#elseif springConfig.migrationTool == "liquibase">
spring.liquibase.enabled=false
</#if>
</#if>

# Server
server.port=8080
