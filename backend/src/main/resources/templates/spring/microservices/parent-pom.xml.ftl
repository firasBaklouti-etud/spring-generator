<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>${request.bootVersion!"3.2.0"}</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>${groupId}</groupId>
	<artifactId>${artifactId}</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>${request.name!"Microservices Project"}</name>
	<description>${request.description!"Spring Cloud Microservices Project"}</description>
	<packaging>pom</packaging>
	<properties>
		<java.version>${request.javaVersion!"17"}</java.version>
<#assign bootVer = (request.bootVersion!"3.2.0")>
<#if bootVer?starts_with("3.4") || bootVer?starts_with("3.5")>
		<spring-cloud.version>2024.0.0</spring-cloud.version>
<#elseif bootVer?starts_with("3.3")>
		<spring-cloud.version>2023.0.3</spring-cloud.version>
<#else>
		<spring-cloud.version>2023.0.0</spring-cloud.version>
</#if>
	</properties>
	<modules>
		<module>discovery-server</module>
		<module>config-server</module>
		<module>api-gateway</module>
<#if services?? && (services?size > 0)>
<#list services as svc>
		<module>${svc.serviceName}</module>
</#list>
</#if>
	</modules>
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${r"${spring-cloud.version}"}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
</project>
