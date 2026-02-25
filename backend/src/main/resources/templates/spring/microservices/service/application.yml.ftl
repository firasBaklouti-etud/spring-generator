server:
  port: ${service.port?c}

spring:
  application:
    name: ${service.serviceName}
<#if hasPostgres?? && hasPostgres>
  datasource:
    url: jdbc:postgresql://localhost:5432/${service.artifactId?replace("-", "_")}
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
<#elseif hasMariadb?? && hasMariadb>
  datasource:
    url: jdbc:mariadb://localhost:3306/${service.artifactId?replace("-", "_")}
    username: root
    password: root
    driver-class-name: org.mariadb.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MariaDBDialect
        format_sql: true
<#elseif hasMysql?? && hasMysql>
  datasource:
    url: jdbc:mysql://localhost:3306/${service.artifactId?replace("-", "_")}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
<#else>
  datasource:
    url: jdbc:h2:mem:${service.artifactId?replace("-", "_")};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
</#if>

eureka:
  client:
    service-url:
      defaultZone: http://localhost:${discoveryPort?c}/eureka/
  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
