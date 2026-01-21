spring.application.name=${request.artifactId}
spring.datasource.url=jdbc:mysql://localhost:3306/${request.artifactId}?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

<#if hasJwt?? && hasJwt>
# JWT Configuration
jwt.secret=${"$"}{JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
jwt.expiration=36000000
jwt.refresh-expiration=604800000
</#if>
