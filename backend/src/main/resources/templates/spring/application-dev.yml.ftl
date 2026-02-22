# ==================== Development Profile ====================
# Activate with: --spring.profiles.active=dev
# Uses H2 in-memory database for rapid local development

spring:
  # H2 In-Memory Database
  datasource:
    url: jdbc:h2:mem:${request.artifactId?replace("-", "_")}_dev;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: ""
    driver-class-name: org.h2.Driver

  # H2 Console (available at /h2-console)
  h2:
    console:
      enabled: true
      path: /h2-console

  # JPA/Hibernate - auto-create tables for dev
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
        globally_quoted_identifiers: true
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool != "none">

  # Disable migrations for dev profile (uses ddl-auto instead)
<#if springConfig.migrationTool == "flyway">
  flyway:
    enabled: false
<#elseif springConfig.migrationTool == "liquibase">
  liquibase:
    enabled: false
</#if>
</#if>

# Server
server:
  port: 8080
