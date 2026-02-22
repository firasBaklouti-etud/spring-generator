# Application Configuration
spring:
  application:
    name: ${request.artifactId}

  # ==================== Database Configuration ====================
<#if request.databaseType?? && request.databaseType == "postgresql">
  # PostgreSQL Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/${request.artifactId?replace("-", "_")}
    username: postgres
    password: ""
    driver-class-name: org.postgresql.Driver
  jpa:
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool != "none">
    hibernate:
      ddl-auto: validate
<#else>
    hibernate:
      ddl-auto: update
</#if>
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        globally_quoted_identifiers: true
<#elseif request.databaseType?? && request.databaseType == "mariadb">
  # MariaDB Configuration
  datasource:
    url: jdbc:mariadb://localhost:3306/${request.artifactId?replace("-", "_")}
    username: root
    password: ""
    driver-class-name: org.mariadb.jdbc.Driver
  jpa:
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool != "none">
    hibernate:
      ddl-auto: validate
<#else>
    hibernate:
      ddl-auto: update
</#if>
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MariaDBDialect
        format_sql: true
        globally_quoted_identifiers: true
<#elseif request.databaseType?? && request.databaseType == "sqlserver">
  # SQL Server Configuration
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=${request.artifactId?replace("-", "_")};encrypt=false
    username: sa
    password: ""
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
  jpa:
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool != "none">
    hibernate:
      ddl-auto: validate
<#else>
    hibernate:
      ddl-auto: update
</#if>
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.SQLServerDialect
        format_sql: true
        globally_quoted_identifiers: true
<#elseif request.databaseType?? && request.databaseType == "h2">
  # H2 In-Memory Database Configuration
  datasource:
    url: jdbc:h2:mem:${request.artifactId?replace("-", "_")};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool != "none">
    hibernate:
      ddl-auto: validate
<#else>
    hibernate:
      ddl-auto: update
</#if>
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
        globally_quoted_identifiers: true
<#else>
  # MySQL Configuration (default)
  datasource:
    url: jdbc:mysql://localhost:3306/${request.artifactId?replace("-", "_")}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: ""
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool != "none">
    hibernate:
      ddl-auto: validate
<#else>
    hibernate:
      ddl-auto: update
</#if>
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
        globally_quoted_identifiers: true
</#if>
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool == "flyway">

  # ==================== Flyway Migrations ====================
  flyway:
    enabled: true
    locations: classpath:db/migration
</#if>
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool == "liquibase">

  # ==================== Liquibase Migrations ====================
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
</#if>
<#if request.securityConfig?? && request.securityConfig.enabled>
<#if request.securityConfig.socialLogins?? && (request.securityConfig.socialLogins?size > 0)>

  # ==================== OAuth2 Social Login ====================
  security:
    oauth2:
      client:
        registration:
<#list request.securityConfig.socialLogins as provider>
<#assign providerLower = provider?lower_case>
<#if providerLower == "google">
          # Register at: https://console.cloud.google.com/apis/credentials
          google:
            client-id: ${"$"}{GOOGLE_CLIENT_ID:your-google-client-id}
            client-secret: ${"$"}{GOOGLE_CLIENT_SECRET:your-google-client-secret}
            scope: openid,profile,email
<#elseif providerLower == "github">
          # Register at: https://github.com/settings/developers
          github:
            client-id: ${"$"}{GITHUB_CLIENT_ID:your-github-client-id}
            client-secret: ${"$"}{GITHUB_CLIENT_SECRET:your-github-client-secret}
            scope: read:user,user:email
<#elseif providerLower == "facebook">
          # Register at: https://developers.facebook.com/apps/
          facebook:
            client-id: ${"$"}{FACEBOOK_CLIENT_ID:your-facebook-client-id}
            client-secret: ${"$"}{FACEBOOK_CLIENT_SECRET:your-facebook-client-secret}
            scope: email,public_profile
</#if>
</#list>
</#if>
<#if request.securityConfig.keycloakEnabled?? && request.securityConfig.keycloakEnabled>

  # ==================== Keycloak Configuration ====================
<#assign keycloakIssuer = request.securityConfig.keycloakIssuerUrl!"http://localhost:8180">
<#assign keycloakRealm = request.securityConfig.keycloakRealm!"my-realm">
<#-- Merge under existing security block or create new one -->
<#if !(request.securityConfig.socialLogins?? && (request.securityConfig.socialLogins?size > 0))>
  security:
    oauth2:
</#if>
      resourceserver:
        jwt:
          issuer-uri: ${keycloakIssuer}/realms/${keycloakRealm}
          jwk-set-uri: ${keycloakIssuer}/realms/${keycloakRealm}/protocol/openid-connect/certs
<#if request.securityConfig.authenticationType == "KEYCLOAK_OAUTH">
<#if !(request.securityConfig.socialLogins?? && (request.securityConfig.socialLogins?size > 0))>
      client:
        registration:
</#if>
          keycloak:
            client-id: ${request.securityConfig.keycloakClientId!"my-client"}
            client-secret: ${request.securityConfig.keycloakClientSecret!"change-me"}
            scope: openid,profile,email
            authorization-grant-type: authorization_code
        provider:
          keycloak:
            issuer-uri: ${keycloakIssuer}/realms/${keycloakRealm}
            user-name-attribute: preferred_username
</#if>
</#if>
<#if request.securityConfig.passwordResetEnabled?? && request.securityConfig.passwordResetEnabled>

  # ==================== Mail Configuration (Password Reset) ====================
  mail:
    host: ${"$"}{MAIL_HOST:smtp.gmail.com}
    port: ${"$"}{MAIL_PORT:587}
    username: ${"$"}{MAIL_USERNAME:your-email@gmail.com}
    password: ${"$"}{MAIL_PASSWORD:your-app-password}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
</#if>
</#if>

# Server Configuration
server:
  port: 8080

<#if request.securityConfig?? && request.securityConfig.enabled>
# ==================== Security Configuration ====================
<#if hasJwt?? && hasJwt>
# JWT Configuration
<#assign isRS256 = (request.securityConfig.signingAlgorithm?? && request.securityConfig.signingAlgorithm == "RS256")>
<#if isRS256>
# RSA256 (Asymmetric) JWT Signing
# Generate keys:
#   openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048
#   openssl rsa -pubout -in private.pem -out public.pem
# Place keys in src/main/resources/keys/
jwt:
  private-key-path: classpath:keys/private.pem
  public-key-path: classpath:keys/public.pem
  expiration: 36000000
  refresh-expiration: 604800000
<#else>
# HMAC-SHA256 (Symmetric) JWT Signing
# SECURITY: Set JWT_SECRET env variable in production with a secure random key
# Generate with: openssl rand -base64 32
jwt:
  secret: ${"$"}{JWT_SECRET:MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}
  expiration: 36000000
  refresh-expiration: 604800000
</#if>
<#else>
# Basic Authentication is enabled
# Configure users in SecurityConfig.java or use database authentication
</#if>

<#if (request.securityConfig.passwordResetEnabled?? && request.securityConfig.passwordResetEnabled) || (request.securityConfig.rememberMeEnabled?? && request.securityConfig.rememberMeEnabled)>
app:
<#if request.securityConfig.passwordResetEnabled?? && request.securityConfig.passwordResetEnabled>
  # Password Reset Settings
  password-reset:
    token-validity-minutes: 60
    base-url: ${"$"}{APP_BASE_URL:http://localhost:8080}
</#if>
<#if request.securityConfig.rememberMeEnabled?? && request.securityConfig.rememberMeEnabled>
  # ==================== Remember-Me ====================
  remember-me:
    key: ${"$"}{REMEMBER_ME_KEY:uniqueAndSecretKey}
    token-validity-seconds: 2592000
</#if>
</#if>
</#if>
<#if request.includeDocker?? && request.includeDocker>

# ==================== Docker Configuration ====================
# When running in Docker, use container hostnames instead of localhost
# spring:
#   datasource:
#     url: jdbc:mysql://db:3306/${request.artifactId?replace("-", "_")}
</#if>
