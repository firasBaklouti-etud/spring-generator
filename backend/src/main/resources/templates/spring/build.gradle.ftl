// Generated Spring Boot Gradle build file
// To use Gradle, initialize the wrapper: gradle wrapper --gradle-version 8.5

plugins {
	id 'java'
	id 'org.springframework.boot' version '${request.bootVersion!"3.2.0"}'
	id 'io.spring.dependency-management' version '1.1.4'
}

group = '${request.groupId}'
version = '0.0.1-SNAPSHOT'

java {
	sourceCompatibility = '${request.javaVersion!"17"}'
}

<#if hasLombok?? && hasLombok>
configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
}

</#if>
repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter'

<#if dependencies?? && (dependencies?size > 0)>
<#list dependencies as dep>
<#if dep.scope?? && dep.scope == "runtime">
	runtimeOnly '${dep.groupId}:${dep.artifactId}<#if dep.version?has_content>:${dep.version}</#if>'
<#elseif dep.scope?? && dep.scope == "test">
	testImplementation '${dep.groupId}:${dep.artifactId}<#if dep.version?has_content>:${dep.version}</#if>'
<#elseif dep.scope?? && dep.scope == "provided">
	compileOnly '${dep.groupId}:${dep.artifactId}<#if dep.version?has_content>:${dep.version}</#if>'
<#else>
	implementation '${dep.groupId}:${dep.artifactId}<#if dep.version?has_content>:${dep.version}</#if>'
</#if>
</#list>
</#if>

	testImplementation 'org.springframework.boot:spring-boot-starter-test'

<#if hasJwt?? && hasJwt>
	// JWT Dependencies
	implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
	runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
	runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'
</#if>

<#if hasSocialLogins?? && hasSocialLogins>
	// OAuth2 Client for Social Logins
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
</#if>

<#if hasKeycloak?? && hasKeycloak>
	// Keycloak Resource Server
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
</#if>

<#if hasPasswordReset?? && hasPasswordReset>
	// Mail for Password Reset
	implementation 'org.springframework.boot:spring-boot-starter-mail'
</#if>

<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool == "flyway">
	// Flyway Database Migrations
	implementation 'org.flywaydb:flyway-core'
<#if request.databaseType?? && (request.databaseType == "mysql" || request.databaseType == "mariadb")>
	implementation 'org.flywaydb:flyway-mysql'
</#if>
</#if>
<#if springConfig?? && springConfig.migrationTool?? && springConfig.migrationTool == "liquibase">
	// Liquibase Database Migrations
	implementation 'org.liquibase:liquibase-core'
</#if>

<#if hasLombok?? && hasLombok>
	compileOnly 'org.projectlombok:lombok'
	annotationProcessor 'org.projectlombok:lombok'
</#if>
}

tasks.named('test') {
	useJUnitPlatform()
<#if request.javaVersion?? && request.javaVersion == "23">
	jvmArgs '-Dnet.bytebuddy.experimental=true'
</#if>
}
