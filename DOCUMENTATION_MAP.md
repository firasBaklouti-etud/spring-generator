# Project Documentation Map

This project is a **Multi-Stack Project Generator** capable of generating Spring Boot, Node.js, NestJS, and FastAPI projects from SQL schemas.

## 📚 Core Documentation

- **[Backend Architecture & API](backend/Documentation.md)**  
  Detailed explanation of the 3-layer architecture, package structure, and API endpoints.

- **[Frontend Architecture & Features](frontend/Documentation.md)**  
  Overview of the Next.js frontend, components, state management, and user flow.

## 🔧 Stack-Specific Documentation

- **[Spring Boot Stack](backend/src/main/java/com/firas/generator/stack/spring/documentation.md)**  
  Implementation details for the Spring Boot generator, including templates and type mapping.

## 🚀 Key Features

*   **Multi-Database SQL Parsing**: Supports MySQL, PostgreSQL, etc.
*   **Visual Schema Editor**: Drag-and-drop entity relationship design.
*   **Multiple Tech Stacks**: Extensible architecture supporting various backend frameworks.
*   **AI Integration**: Generate schemas and tables using AI.
*   **IDE Preview**: Edit generated code before downloading.
*   **Dual-Mode RBAC**: Static (Enum-based) and Dynamic (Database-driven) Role-Based Access Control.

## 🔐 Security & RBAC Documentation

- **[RBAC Implementation Status](RBAC_STATUS.md)**  
  Current status of the Dual-Mode RBAC feature implementation.

- **[RBAC Implementation Guide](backend/RBAC_GUIDE.md)**  
  Comprehensive guide for Static and Dynamic RBAC modes.

### RBAC Architecture Overview

The Spring Generator supports two RBAC modes:

#### Static RBAC (Compile-time)
- **Permission.java**: Enum containing all permissions (e.g., `USER_READ`, `PRODUCT_WRITE`)
- **Role.java**: Enum with permission sets and `getAuthorities()` method
- **Entity Integration**: Principal entity's `getAuthorities()` delegates to Role enum
- **Controller Security**: `@PreAuthorize` annotations generated based on security rules

#### Dynamic RBAC (Runtime)
- **Role Entity**: JPA entity with `@ElementCollection` for permissions stored in database
- **RoleRepository**: Spring Data repository for Role entity
- **ManyToMany Relationship**: Auto-injected between User and Role entities
- **Controller Security**: `@PreAuthorize` annotations support both modes

### Template Structure

| Template | Purpose | Mode |
|----------|---------|------|
| `Permission.ftl` | Permission enum generation | Static |
| `Role.ftl` | Role enum generation | Static |
| `RoleEntity.ftl` | Role JPA entity generation | Dynamic |
| `Controller.ftl` | REST controller with `@PreAuthorize` | Both |
| `Entity.ftl` | JPA entity with `getAuthorities()` | Both |

### Metadata Injection Flow

1. **SpringStackProvider.generateProject()** identifies principal entity
2. Injects metadata: `isUserDetails`, `rbacMode`, `roleField`, `usernameField`, `passwordField`
3. For Dynamic mode, injects ManyToMany relationship to Role entity
4. **SpringCodeGenerator** receives security config for `@PreAuthorize` generation
5. Templates use metadata to generate appropriate code

## 🗺️ Quick Links

*   [Backend Source Code](backend/Documentation.md)
*   [Frontend Source Code](frontend/Documentation.md)
*   [RBAC Status Tracking](RBAC_STATUS.md)
