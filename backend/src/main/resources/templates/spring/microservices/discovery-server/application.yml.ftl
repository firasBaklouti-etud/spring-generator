server:
  port: ${discoveryPort?c}

spring:
  application:
    name: discovery-server

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://localhost:${discoveryPort?c}/eureka/
  server:
    enable-self-preservation: false
