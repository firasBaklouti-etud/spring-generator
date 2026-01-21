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
	<groupId>${request.groupId}</groupId>
	<artifactId>${request.artifactId}</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>${request.name}</name>
	<description>${request.description!"Generated Spring Boot Project"}</description>
	<properties>
		<java.version>${request.javaVersion!"17"}</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter</artifactId>
		</dependency>
        
        <#if dependencies?? && (dependencies?size > 0)>
        <#list dependencies as dep>
        <dependency>
            <groupId>${dep.groupId}</groupId>
            <artifactId>${dep.artifactId}</artifactId>
            <#if dep.version??>
            <version>${dep.version}</version>
            </#if>
            <#if dep.scope??>
            <scope>${dep.scope}</scope>
            </#if>
        </dependency>
        </#list>
        </#if>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>

		<#if hasJwt?? && hasJwt>
		<!-- JWT Dependencies -->
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-api</artifactId>
			<version>0.11.5</version>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-impl</artifactId>
			<version>0.11.5</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-jackson</artifactId>
			<version>0.11.5</version>
			<scope>runtime</scope>
		</dependency>
		</#if>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<#if hasLombok?? && hasLombok>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
				</#if>
			</plugin>
		</plugins>
	</build>

</project>
