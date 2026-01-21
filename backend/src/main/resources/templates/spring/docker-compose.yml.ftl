version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ${request.artifactId!"spring-app"}
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
<#if hasMysql?? && hasMysql>
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/${request.artifactId!"springdb"}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=root
<#elseif hasPostgres?? && hasPostgres>
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${request.artifactId!"springdb"}
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
<#elseif hasMariadb?? && hasMariadb>
      - SPRING_DATASOURCE_URL=jdbc:mariadb://mariadb:3306/${request.artifactId!"springdb"}
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=root
<#else>
      - SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb
</#if>
<#if hasJwt?? && hasJwt>
      # SECURITY: Set JWT_SECRET env variable in production with a secure random key
      # Generate with: openssl rand -base64 32
      - JWT_SECRET=${r"${JWT_SECRET:-CHANGE_THIS_SECRET_IN_PRODUCTION}"}
      - JWT_EXPIRATION=36000000
      - JWT_REFRESH_EXPIRATION=604800000
</#if>
    depends_on:
<#if hasMysql?? && hasMysql>
      mysql:
        condition: service_healthy
<#elseif hasPostgres?? && hasPostgres>
      postgres:
        condition: service_healthy
<#elseif hasMariadb?? && hasMariadb>
      mariadb:
        condition: service_healthy
</#if>
    networks:
      - spring-network
    restart: unless-stopped

<#if hasMysql?? && hasMysql>
  mysql:
    image: mysql:8.0
    container_name: ${request.artifactId!"spring"}-mysql
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=${request.artifactId!"springdb"}
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
      - spring-network
    restart: unless-stopped

</#if>
<#if hasPostgres?? && hasPostgres>
  postgres:
    image: postgres:15-alpine
    container_name: ${request.artifactId!"spring"}-postgres
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_DB=${request.artifactId!"springdb"}
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
      - spring-network
    restart: unless-stopped

</#if>
<#if hasMariadb?? && hasMariadb>
  mariadb:
    image: mariadb:10.11
    container_name: ${request.artifactId!"spring"}-mariadb
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=${request.artifactId!"springdb"}
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
      - spring-network
    restart: unless-stopped

</#if>
<#if hasRedis?? && hasRedis>
  redis:
    image: redis:7-alpine
    container_name: ${request.artifactId!"spring"}-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - spring-network
    restart: unless-stopped

</#if>
<#if hasMongodb?? && hasMongodb>
  mongodb:
    image: mongo:6
    container_name: ${request.artifactId!"spring"}-mongodb
    ports:
      - "27017:27017"
    volumes:
      - mongodb-data:/data/db
    networks:
      - spring-network
    restart: unless-stopped

</#if>
networks:
  spring-network:
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
<#if hasRedis?? && hasRedis>
  redis-data:
</#if>
<#if hasMongodb?? && hasMongodb>
  mongodb-data:
</#if>
