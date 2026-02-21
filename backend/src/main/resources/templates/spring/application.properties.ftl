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
<#assign isRS256 = (request.securityConfig.signingAlgorithm?? && request.securityConfig.signingAlgorithm == "RS256")>
<#if isRS256>
# RSA256 (Asymmetric) JWT Signing
# Generate keys:
#   openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048
#   openssl rsa -pubout -in private.pem -out public.pem
# Place keys in src/main/resources/keys/
jwt.private-key-path=classpath:keys/private.pem
jwt.public-key-path=classpath:keys/public.pem
<#else>
# HMAC-SHA256 (Symmetric) JWT Signing
# SECURITY: Set JWT_SECRET env variable in production with a secure random key
# Generate with: openssl rand -base64 32
jwt.secret=${"$"}{JWT_SECRET:MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}
</#if>
jwt.expiration=36000000
jwt.refresh-expiration=604800000
<#else>
# Basic Authentication is enabled
# Configure users in SecurityConfig.java or use database authentication
</#if>

<#-- Social Login OAuth2 Configuration -->
<#if request.securityConfig.socialLogins?? && (request.securityConfig.socialLogins?size > 0)>
# ==================== OAuth2 Social Login ====================
<#list request.securityConfig.socialLogins as provider>
<#assign providerLower = provider?lower_case>
# ${provider} OAuth2 Configuration
# Register your app at:
<#if providerLower == "google">
# https://console.cloud.google.com/apis/credentials
spring.security.oauth2.client.registration.google.client-id=${"$"}{GOOGLE_CLIENT_ID:your-google-client-id}
spring.security.oauth2.client.registration.google.client-secret=${"$"}{GOOGLE_CLIENT_SECRET:your-google-client-secret}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
<#elseif providerLower == "github">
# https://github.com/settings/developers
spring.security.oauth2.client.registration.github.client-id=${"$"}{GITHUB_CLIENT_ID:your-github-client-id}
spring.security.oauth2.client.registration.github.client-secret=${"$"}{GITHUB_CLIENT_SECRET:your-github-client-secret}
spring.security.oauth2.client.registration.github.scope=read:user,user:email
<#elseif providerLower == "facebook">
# https://developers.facebook.com/apps/
spring.security.oauth2.client.registration.facebook.client-id=${"$"}{FACEBOOK_CLIENT_ID:your-facebook-client-id}
spring.security.oauth2.client.registration.facebook.client-secret=${"$"}{FACEBOOK_CLIENT_SECRET:your-facebook-client-secret}
spring.security.oauth2.client.registration.facebook.scope=email,public_profile
</#if>

</#list>
</#if>

<#-- Keycloak Configuration -->
<#if request.securityConfig.keycloakEnabled?? && request.securityConfig.keycloakEnabled>
# ==================== Keycloak Configuration ====================
<#assign keycloakIssuer = request.securityConfig.keycloakIssuerUrl!"http://localhost:8180">
<#assign keycloakRealm = request.securityConfig.keycloakRealm!"my-realm">
spring.security.oauth2.resourceserver.jwt.issuer-uri=${keycloakIssuer}/realms/${keycloakRealm}
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${keycloakIssuer}/realms/${keycloakRealm}/protocol/openid-connect/certs
<#if request.securityConfig.authenticationType == "KEYCLOAK_OAUTH">
spring.security.oauth2.client.registration.keycloak.client-id=${request.securityConfig.keycloakClientId!"my-client"}
spring.security.oauth2.client.registration.keycloak.client-secret=${request.securityConfig.keycloakClientSecret!"change-me"}
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.provider.keycloak.issuer-uri=${keycloakIssuer}/realms/${keycloakRealm}
spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username
</#if>
</#if>

<#-- Password Reset / Mail Configuration -->
<#if request.securityConfig.passwordResetEnabled?? && request.securityConfig.passwordResetEnabled>
# ==================== Mail Configuration (Password Reset) ====================
spring.mail.host=${"$"}{MAIL_HOST:smtp.gmail.com}
spring.mail.port=${"$"}{MAIL_PORT:587}
spring.mail.username=${"$"}{MAIL_USERNAME:your-email@gmail.com}
spring.mail.password=${"$"}{MAIL_PASSWORD:your-app-password}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Password Reset Settings
app.password-reset.token-validity-minutes=60
app.password-reset.base-url=${"$"}{APP_BASE_URL:http://localhost:8080}
</#if>

<#-- Remember-Me Configuration -->
<#if request.securityConfig.rememberMeEnabled?? && request.securityConfig.rememberMeEnabled>
# ==================== Remember-Me ====================
app.remember-me.key=${"$"}{REMEMBER_ME_KEY:uniqueAndSecretKey}
app.remember-me.token-validity-seconds=2592000
</#if>
</#if>

<#if request.includeDocker?? && request.includeDocker>
# ==================== Docker Configuration ====================
# When running in Docker, use container hostnames instead of localhost
# spring.datasource.url=jdbc:mysql://db:3306/${request.artifactId?replace("-", "_")}
</#if>
