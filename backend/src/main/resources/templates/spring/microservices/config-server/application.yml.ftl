server:
  port: ${configPort?c}

spring:
  application:
    name: config-server
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/configurations

eureka:
  client:
    service-url:
      defaultZone: http://localhost:${discoveryPort?c}/eureka/
