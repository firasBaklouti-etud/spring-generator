spring.application.name=${request.artifactId}
spring.datasource.url=jdbc:mysql://localhost:3306/${request.artifactId}?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

<#if hasJwt?? && hasJwt>
# JWT Configuration
# SECURITY: Set JWT_SECRET env variable in production with a secure random key
# Generate with: openssl rand -base64 32
jwt.secret=${"$"}{JWT_SECRET:CHANGE_THIS_SECRET_IN_PRODUCTION}
jwt.expiration=36000000
jwt.refresh-expiration=604800000
</#if>
