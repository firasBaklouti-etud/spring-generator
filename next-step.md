# Next Steps — project Generator MCP

> Comprehensive list of bugs, improvements, and feature ideas.
> Organized by priority. Compiled from MCP tool testing + full source code review.

---

## 🔴 P0 — Critical Bugs (Generated code won't compile)

### 1. Double Package Suffix in All Templates
**Files:** `Entity.ftl`, `Service.ftl`, `Repository.ftl`, `Controller.ftl`, `Dto.ftl`, `Mapper.ftl`

Templates append the layer suffix (`.entity`, `.service`, etc.) to `packageName`, but `getEffectivePackage()` already includes it. Result:
```
package com.example.demo.entity.entity;   // WRONG
package com.example.demo.service.service; // WRONG
```
**Fix:** Templates should use `package ${packageName};` without appending the layer suffix, OR `getEffectivePackage()` should stop appending it.

### 2. Cross-Package Imports Broken for Non-LAYERED Structures
**Files:** `Controller.ftl`, `Service.ftl`

Controller imports `${packageName}.entity.${className}` — but for FEATURE/DDD/HEXAGONAL structures, `packageName` is the controller's effective package, not the base package. These imports resolve to nonexistent packages.

**Fix:** Pass `basePackageName` to all templates (like `Mapper.ftl` already does) and use it for cross-layer imports.

### 3. Thread-Unsafe Singleton State
**File:** `SpringCodeGenerator.java` (lines 39-40)

`securityConfig` and `springConfig` are mutable instance fields on a `@Component` singleton. Two concurrent requests will overwrite each other's configuration.

**Fix:** Pass config as method parameters, or use a request-scoped context object.

### 4. `dependencyMap` Never Populated
**File:** `SpringDependencyProvider.java` (line 26)

`initializeDynamicDependencies()` never calls `dependencyMap.put(...)`. Every call to `getDependencyById()` returns `null`.

**Fix:** Add `dependencyMap.put(dep.getId(), dep)` inside the initialization loop.

### 5. Missing Critical Dependencies in Generated `pom.xml`
**File:** `pom.xml.ftl` / `SpringStackProvider.java`

Generated projects are missing:
- `spring-boot-starter-security` (needed for SecurityFilterChain)
- `spring-boot-starter-data-jpa` (needed for @Entity, JpaRepository)
- `spring-boot-starter-web` (needed for @RestController)
- `mysql-connector-j` (needed by datasource config)

**Fix:** Auto-include these based on project features (security enabled → add security starter, tables present → add JPA + driver).

### 6. Duplicate Methods in Generated `Users.java`
`getPassword()` and `getUsername()` are defined twice — once as POJO getters, once as `UserDetails` overrides. Won't compile.

**Fix:** Remove the duplicate POJO getters when the entity implements `UserDetails`, or merge them.

### 7. Missing `findByEmail` in Generated Repository
`CustomUserDetailsService` calls `repository.findByEmail(username)`, but the generated `UsersRepository` doesn't declare this method.

**Fix:** When security is enabled with `usernameField = "email"`, auto-add `Optional<Users> findByEmail(String email)` to the repository.

---

## 🟠 P1 — Major Issues (Functional gaps / anti-patterns)

### 8. No PUT/PATCH Endpoint
`Controller.ftl` generates GET, POST, DELETE but **no update endpoint**. `Mapper.ftl` generates `updateEntity()` but it's never wired into the service or controller.

**Fix:** Add PUT endpoint in controller + `update()` method in service that uses the mapper.

### 9. No Primary Key Fallback in Templates
If a table has no PK defined, the template renders `JpaRepository<User, >` (empty type parameter) — invalid Java.

**Fix:** Default to `Long` if no PK is found, or throw a clear validation error during generation.

### 10. Composite Primary Keys Not Supported
Multiple PK columns produce concatenated types like `IntegerLong`. No `@EmbeddedId` / `@IdClass` support.

**Fix:** Detect composite PKs and generate an `@IdClass` or `@EmbeddedId` wrapper.

### 11. `ManyToMany` Always Gets `FetchType.EAGER`
`Entity.ftl` hardcodes `FetchType.EAGER` for all `@ManyToMany` relationships — not just roles. This is an N+1 anti-pattern for general M:N relations like students↔courses.

**Fix:** Default to `FetchType.LAZY` for all relationships. Only use EAGER for security roles (if security is enabled).

### 12. App Crashes if `start.spring.io` Is Unreachable
`SpringDependencyProvider.java` — `@PostConstruct` throws `RuntimeException` if the external API call fails. The entire app won't start.

**Fix:** Catch the exception, fall back to a bundled/cached dependency list, and log a warning.

### 13. DTOs Generated but Never Used
DTO and Mapper classes are generated, but Controller and Service still use raw entities for request/response.

**Fix:** Wire DTOs into controller endpoints. Controller should accept/return DTOs, service converts via mapper.

### 14. No `@Valid` / Bean Validation Annotations
No `@Valid` on `@RequestBody` in controllers. No `@NotNull`, `@Size`, `@Email` on entity/DTO fields despite having nullable/unique metadata from the schema.

**Fix:** Map SQL constraints to validation annotations: `NOT NULL` → `@NotNull`, `VARCHAR(255)` → `@Size(max=255)`, unique → document it.

### 15. No Global Exception Handler
No `@RestControllerAdvice` template. Errors return raw Spring stack traces or empty 404s.

**Fix:** Generate a `GlobalExceptionHandler` with handlers for `EntityNotFoundException`, `MethodArgumentNotValidException`, `AccessDeniedException`, etc.

### 16. No CORS Configuration
No `WebMvcConfigurer` for CORS. Frontend-backend projects will fail on cross-origin requests.

**Fix:** Generate a `CorsConfig.java` with configurable allowed origins (default: `http://localhost:3000`).

### 17. Typo in Generated Endpoint Path
`UsersController` maps to `/api/userss` (double 's').

**Fix:** Fix pluralization logic in template or `SqlParser.plural()`.

### 18. Duplicated Type Mapping Logic
`SqlParser.java` has its own `mapJavaType()` that diverges from `SpringTypeMapper.java`:
- `DATE` → `LocalDateTime` (should be `LocalDate`)
- `TINYINT` → `Boolean` vs `Integer`

**Fix:** Remove `mapJavaType()` from `SqlParser` and always use the stack's TypeMapper via `applyTypeMappings()`.

---

## 🟡 P2 — Important Improvements

### 19. No `.gitignore` in Generated Project
Every generated project should include a `.gitignore` for `target/`, `*.class`, `.idea/`, `.vscode/`, `*.iml`, `.env`, etc.

### 20. No `README.md` in Generated Project
Generate a README with: project name, description, how to build/run, API endpoints, database setup instructions.

### 21. No Maven Wrapper (`mvnw`)
Generated projects should include the Maven wrapper so users don't need Maven globally installed.

### 22. No Pagination / Sorting Support
`service.findAll()` returns `List<T>`. Standard Spring Boot APIs should support `Pageable` → `Page<T>`.

**Fix:** Add optional paginated `findAll(Pageable pageable)` in service and `?page=0&size=20` in controller.

### 23. No Service Test Generation
`SpringCodeGenerator` generates `RepositoryTest` and `ControllerTest` but no `ServiceTest`. The service layer (often the most critical) goes untested.

### 24. Entity Imports Are Over-Inclusive / Under-Inclusive
`Entity.ftl` always imports `LocalDate` and `LocalDateTime` even if unused. Missing imports for `BigDecimal`, `UUID`, `byte[]` which the type mapper can produce.

**Fix:** Conditionally import based on actual column types used.

### 25. Test Templates Have Placeholder TODOs
`ControllerTest.ftl` and `RepositoryTest.ftl` contain `/* TODO: set test value */` — generated tests won't set field values and test with null entities.

**Fix:** Generate sample values based on column type (e.g., `"test"` for String, `1L` for Long, `true` for Boolean).

### 26. No `application.yml` Option
Only `application.properties` is generated. Many teams prefer YAML format.

### 27. `list_dependencies` Returns 80KB Payload
Too large for LLM context windows. Wastes tokens.

**Fix:** Add `category` filter parameter (e.g., `list_dependencies(category="Web")`). Or paginate the response.

### 28. `configure_security` Doesn't Validate `principalEntity`
No check that the `principalEntity` matches an existing parsed table name. User can set `principalEntity = "Foo"` with no `Foo` table.

**Fix:** Validate against the tables in the project request, or at minimum warn.

### 29. `preview_project` Returns Full File Content
Should return just file paths and sizes for the preview. Full content should be opt-in to save tokens.

### 30. No Swagger / OpenAPI Auto-Configuration
No `springdoc-openapi` dependency or configuration. API documentation should be generated automatically.

**Fix:** Add option to include `springdoc-openapi-starter-webmvc-ui` and generate an `OpenApiConfig.java`.

### 31. Hardcoded MySQL Config with root/no-password
Generated `application.properties` assumes MySQL with `root` and empty password. No H2 dev profile.

**Fix:** Generate a `application-dev.properties` with H2 in-memory config for instant dev experience.

### 32. Controller Indentation Bug
`Controller.ftl` and `Service.ftl` — constructor body starts at column 0 inside the class:
```java
    public UsersController(UsersService service) {
    this.service = service;  // should be indented
    }
```

---

## 🔵 P3 — Feature Ideas (New Capabilities)

### 33. Support OpenAPI Spec as Alternative Input
Currently only SQL is accepted as input. Many developers design API-first.

**Add:** `parse_openapi` tool — accepts OpenAPI YAML/JSON, generates endpoints + DTOs from it. Pairs with `parse_sql` for full coverage.

### 34. Database Migration Support (Flyway / Liquibase)
Already #1 on ROADMAP.md. Essential for production use. Generate migration files from schema diff.

### 35. Implement Node / Nest / FastAPI Stacks
`StackType` enum declares them, `ListDependenciesTool` advertises them, but no implementation exists. Selecting non-SPRING throws at runtime.

**Fix (short-term):** Remove from MCP tool schema until implemented.
**Fix (long-term):** Implement at least one alternative stack (FastAPI is the easiest — smallest surface area).

### 36. Gradle Build Support
`SpringConfig.buildTool` accepts `"gradle"` but only `pom.xml.ftl` exists. No `build.gradle.ftl`.

### 37. Docker File Generation via MCP
`ProjectRequest.includeDocker` flag exists. Docker templates may exist. But no MCP tool exposes this option.

**Fix:** Add `includeDocker` to the MCP `generate_project` / `preview_project` tool schema.

### 38. Add `validate_project` MCP Tool
New tool that takes a `ProjectRequest` and returns validation errors *before* generation:
- Missing PK warnings
- Invalid package names
- Security config referencing non-existent entities
- Unsupported stack type

### 39. Add `customize_template` MCP Tool
Allow users to override specific templates (e.g., "use Lombok on entities", "use records for DTOs") without forking the generator.

### 40. CQRS / Event Sourcing Templates
For advanced architectures. Generate Command/Query separation patterns, Event classes, Event handlers.

### 41. Multi-Module Project Generation
Generate parent POM + child modules (e.g., `api`, `service`, `domain`, `infrastructure`). Important for DDD.

### 42. Audit Fields Generation
Optional `createdAt`, `updatedAt`, `createdBy`, `updatedBy` fields on all entities with `@EntityListeners(AuditingEntityListener.class)`.

### 43. Soft Delete Support
Optional `@SQLDelete` / `@Where(clause = "deleted = false")` pattern for entities.

### 44. API Versioning Support
Generate versioned controllers: `/api/v1/users`, `/api/v2/users` with configurable strategy (URL path, header, query param).

---

## 🟣 Architecture Refactoring

### 45. Decompose `SpringStackProvider` (God Class)
At 563 lines, it handles too many concerns. Split into:
- `PomXmlGenerator`
- `ApplicationPropertiesGenerator`
- `SecurityCodeGenerator`
- `DockerFileGenerator`
- `MigrationFileGenerator`

### 46. Stop Mutating Input `ProjectRequest`
Security preprocessing modifies `Table` objects in-place (adds password column, roles relationship). This means the `ProjectRequest` can't be reused for preview→generate flow.

**Fix:** Clone tables before mutation, or use a separate `ProcessedProjectRequest`.

### 47. RoleRepository Uses String Concatenation
`generateRoleRepository()` builds Java source via string concatenation instead of a FreeMarker template. Inconsistent with the rest of the architecture.

**Fix:** Create `RoleRepository.ftl` template.

### 48. `CodeGenerator.generateAllCrud()` Default Method Is Dead Code
`CodeGenerator.java` has a `default generateAllCrud()` that duplicates the loop in `SpringStackProvider.generateProject()`. It's never called.

**Fix:** Either use it or remove it.

### 49. `applyTypeMappings()` Overwrites User-Provided Types
If a user manually provides `javaType` via MCP (without SQL parsing), `applyTypeMappings()` unconditionally overwrites it.

**Fix:** Only overwrite if `javaType` is null or empty.

### 50. No JDBC Connection Cleanup
`SqlParser.loadMetadata(Connection)` receives a `Connection` but never closes it. Potential resource leak.

**Fix:** Use try-with-resources or ensure caller closes connection in a `finally` block.

### 51. Exception Swallowing in SqlParser
`catch (Exception ignore) {}` silently swallows all exceptions during unique index loading.

**Fix:** At minimum `log.debug("Failed to load unique indexes", e)`.

### 52. Custom `Supplier` Interface Shadows `java.util.function.Supplier`
`SqlParser.java` defines a private `Supplier<T>` that throws checked exceptions.

**Fix:** Rename to `ThrowingSupplier<T>`.

---

## 📝 Documentation Debt

### 53. README.md Is Outdated
- Still references REST API endpoints (`/api/dependencies/groups`) — actual API is MCP-based
- Lists "JWT Authentication" and "Docker Compose" as future — both are already implemented
- Architecture diagram doesn't mention MCP, stack abstraction, or security module

### 54. RBAC_STATUS.md Claims ~95% Completion
States "Generated projects compile and run successfully" as the sole unchecked criterion. Given the double-package-suffix bug, generated projects **do not compile** for non-LAYERED structures.

### 55. No MCP Tool Documentation
No doc explaining what each MCP tool does, what parameters it accepts, and what it returns. Users (and AI agents) have to guess from the JSON schema.

**Fix:** Add a `MCP_TOOLS.md` with examples for each tool.

---

## ✅ Suggested Execution Order

| Phase | Items | Goal |
|-------|-------|------|
| **Phase 1: Make it compile** | #1, #2, #4, #5, #6, #7, #17, #32 | Generated code compiles on first try |
| **Phase 2: Make it production-ready** | #3, #8, #12, #13, #14, #15, #16, #18 | Real-world usable output |
| **Phase 3: Polish** | #9, #10, #11, #19, #20, #21, #22, #24, #25, #30, #31 | Professional quality |
| **Phase 4: Developer experience** | #23, #26, #27, #28, #29, #38, #53, #54, #55 | Better DX for MCP users |
| **Phase 5: Architecture** | #45, #46, #47, #48, #49, #50, #51, #52 | Clean, maintainable codebase |
| **Phase 6: New features** | #33, #34, #35, #36, #37, #39, #40, #41, #42, #43, #44 | Competitive feature set |
