server:
  port: ${gatewayPort?c}

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
<#if services?? && (services?size > 0)>
<#list services as svc>
        - id: ${svc.serviceName}
          uri: lb://${svc.serviceName?upper_case?replace("-", "-")}
          predicates:
            - Path=/api/${svc.serviceName?replace("-service", "")}/**
          filters:
            - StripPrefix=1
</#list>
</#if>

eureka:
  client:
    service-url:
      defaultZone: http://localhost:${discoveryPort?c}/eureka/

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
