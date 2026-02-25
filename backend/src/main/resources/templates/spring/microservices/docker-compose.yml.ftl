version: '3.8'

services:
  discovery-server:
    build:
      context: ./discovery-server
      dockerfile: Dockerfile
    container_name: discovery-server
    ports:
      - "${discoveryPort?c}:${discoveryPort?c}"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    networks:
      - microservices-network
    restart: unless-stopped

  config-server:
    build:
      context: ./config-server
      dockerfile: Dockerfile
    container_name: config-server
    ports:
      - "${configPort?c}:${configPort?c}"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-server:${discoveryPort?c}/eureka/
    depends_on:
      - discovery-server
    networks:
      - microservices-network
    restart: unless-stopped

  api-gateway:
    build:
      context: ./api-gateway
      dockerfile: Dockerfile
    container_name: api-gateway
    ports:
      - "${gatewayPort?c}:${gatewayPort?c}"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-server:${discoveryPort?c}/eureka/
    depends_on:
      - discovery-server
      - config-server
    networks:
      - microservices-network
    restart: unless-stopped

<#if services?? && (services?size > 0)>
<#list services as svc>
  ${svc.serviceName}:
    build:
      context: ./${svc.serviceName}
      dockerfile: Dockerfile
    container_name: ${svc.serviceName}
    ports:
      - "${svc.port?c}:${svc.port?c}"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-server:${discoveryPort?c}/eureka/
<#if hasMysql?? && hasMysql>
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/${svc.artifactId?replace("-", "_")}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=root
<#elseif hasPostgres?? && hasPostgres>
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${svc.artifactId?replace("-", "_")}
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
<#elseif hasMariadb?? && hasMariadb>
      - SPRING_DATASOURCE_URL=jdbc:mariadb://mariadb:3306/${svc.artifactId?replace("-", "_")}
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=root
</#if>
    depends_on:
      - discovery-server
      - config-server
<#if hasMysql?? && hasMysql>
      - mysql
<#elseif hasPostgres?? && hasPostgres>
      - postgres
<#elseif hasMariadb?? && hasMariadb>
      - mariadb
</#if>
    networks:
      - microservices-network
    restart: unless-stopped

</#list>
</#if>
<#if hasMysql?? && hasMysql>
  mysql:
    image: mysql:8.0
    container_name: ${request.artifactId!"microservices"}-mysql
    environment:
      - MYSQL_ROOT_PASSWORD=root
<#if services?? && (services?size > 0)>
<#list services as svc>
      - MYSQL_DATABASE=${svc.artifactId?replace("-", "_")}
<#break>
</#list>
</#if>
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - microservices-network
    restart: unless-stopped

</#if>
<#if hasPostgres?? && hasPostgres>
  postgres:
    image: postgres:15-alpine
    container_name: ${request.artifactId!"microservices"}-postgres
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
<#if services?? && (services?size > 0)>
<#list services as svc>
      - POSTGRES_DB=${svc.artifactId?replace("-", "_")}
<#break>
</#list>
</#if>
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - microservices-network
    restart: unless-stopped

</#if>
<#if hasMariadb?? && hasMariadb>
  mariadb:
    image: mariadb:10.11
    container_name: ${request.artifactId!"microservices"}-mariadb
    environment:
      - MYSQL_ROOT_PASSWORD=root
<#if services?? && (services?size > 0)>
<#list services as svc>
      - MYSQL_DATABASE=${svc.artifactId?replace("-", "_")}
<#break>
</#list>
</#if>
    ports:
      - "3306:3306"
    volumes:
      - mariadb-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - microservices-network
    restart: unless-stopped

</#if>
networks:
  microservices-network:
    driver: bridge

volumes:
<#if hasMysql?? && hasMysql>
  mysql-data:
</#if>
<#if hasPostgres?? && hasPostgres>
  postgres-data:
</#if>
<#if hasMariadb?? && hasMariadb>
  mariadb-data:
</#if>
  discovery-data:
