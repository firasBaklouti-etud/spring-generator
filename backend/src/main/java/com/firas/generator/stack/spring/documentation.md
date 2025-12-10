# Spring Stack Provider Documentation

This document describes the **Spring Boot** implementation of the [Multi-Stack Architecture](../../Documentation.md). It serves as the reference implementation for the `StackProvider` interface.

## Overview

The Spring Stack Provider generates complete Spring Boot projects with:
- Maven build configuration (`pom.xml`)
- Main Application class
- JPA Entities with relationships
- Spring Data JPA Repositories
- Service layer classes
- REST Controllers
- Application properties

## Components

### SpringStackProvider
**Location**: `com.firas.generator.stack.spring.SpringStackProvider`

Main entry point for Spring project generation. Implements `StackProvider` interface.

**Key Methods**:
- `generateProject(ProjectRequest)` - Returns `List<FilePreview>` for IDE preview
- `generateProjectZip(ProjectRequest)` - Returns ZIP byte array

### SpringCodeGenerator
**Location**: `com.firas.generator.stack.spring.SpringCodeGenerator`

Generates CRUD code files using FreeMarker templates.

**Templates Used** (in `resources/templates/spring/`):
- `Entity.ftl` - JPA entity with relationships
- `Repository.ftl` - Spring Data JPA interface
- `Service.ftl` - Business logic layer
- `Controller.ftl` - REST endpoints

### SpringTypeMapper
**Location**: `com.firas.generator.stack.spring.SpringTypeMapper`

Maps SQL types to Java types:

| SQL Type | Java Type |
|----------|-----------|
| VARCHAR, TEXT | String |
| INT, INTEGER | Integer |
| BIGINT | Long |
| DECIMAL, NUMERIC | BigDecimal |
| BOOLEAN, BIT | Boolean |
| DATE | LocalDate |
| TIMESTAMP, DATETIME | LocalDateTime |

### SpringDependencyProvider
**Location**: `com.firas.generator.stack.spring.SpringDependencyProvider`

Wraps `DependencyRegistry` which fetches dependencies from `start.spring.io`.

**API Exposure**:
These dependencies are exposed via the `/api/dependencies/groups?stackType=SPRING` endpoint, which the frontend consumes directly to populate the dependency selection modal. This ensures the frontend always has the latest valid dependencies without relying on hardcoded mock data.

## SpringConfig

**Location**: `com.firas.generator.model.config.SpringConfig`

Spring-specific configuration fields:

```java
public class SpringConfig {
    private String groupId = "com.example";
    private String artifactId = "demo";
    private String javaVersion = "17";
    private String bootVersion = "3.2.0";
    private String buildTool = "maven";
    private String packaging = "jar";
}
```

## Templates

Templates are located in `resources/templates/spring/`:

| Template | Output |
|----------|--------|
| `pom.xml.ftl` | Maven project configuration |
| `Application.java.ftl` | Main Spring Boot class |
| `application.properties.ftl` | Configuration properties |
| `Entity.ftl` | JPA entity class |
| `Repository.ftl` | Repository interface |
| `Service.ftl` | Service class |
| `Controller.ftl` | REST controller |

## Adding New Features

To add new templates:

1. Create `.ftl` file in `templates/spring/`
2. Add generation method in `SpringCodeGenerator`
3. Call from `SpringStackProvider.generateProject()`
