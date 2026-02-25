# Implementation Plan: Microservices Architecture + Frontend Generation

## Overview

Add two major features to the project generator:
1. **Microservices Architecture** - Generate multi-module Spring Cloud projects instead of monoliths
2. **Frontend Generation** - Generate Next.js frontends using a Factory pattern (extensible to Angular, React, etc.)

Both features require backend model changes, new Java services, FreeMarker templates, frontend UI updates, and documentation/test updates.

---

## PHASE 1: Backend Model Layer (Foundation)

All new data model classes needed by both features. No generation logic.

### 1.1 Create `ArchitectureType` enum
**New file**: `backend/src/main/java/com/firas/generator/model/config/ArchitectureType.java`
- Values: `MONOLITH` (default), `MICROSERVICES`
- Include `id`, `displayName`, `fromId()` static method

### 1.2 Create `MicroservicesConfig` model
**New file**: `backend/src/main/java/com/firas/generator/model/config/MicroservicesConfig.java`
- `mode`: `"AUTO"` | `"MANUAL"` (default AUTO)
- `serviceTableMapping`: `Map<String, List<String>>` - for MANUAL mode, maps service names to table names
- `discoveryPort`: int (default 8761)
- `configPort`: int (default 8888)
- `gatewayPort`: int (default 8080)
- `serviceStartPort`: int (default 8081)

### 1.3 Create `ServiceDefinition` internal model
**New file**: `backend/src/main/java/com/firas/generator/model/config/ServiceDefinition.java`
- `serviceName`: String (e.g., "user-service")
- `artifactId`: String
- `port`: int
- `tables`: `List<Table>`
- `packageName`: String (e.g., "com.example.userservice")
- Used internally by MicroservicesGenerator; not part of API contract

### 1.4 Create `FrontendConfig` model
**New file**: `backend/src/main/java/com/firas/generator/model/config/FrontendConfig.java`
- `enabled`: boolean (default false)
- `framework`: String (default "NEXTJS") -- values: NEXTJS, ANGULAR, REACT
- `port`: int (default 3000)
- `backendUrl`: String (default "http://localhost:8080")

### 1.5 Add fields to `SpringConfig`
**Modify**: `backend/src/main/java/com/firas/generator/model/config/SpringConfig.java`
- Add `architectureType`: ArchitectureType (default MONOLITH)
- Add `microservicesConfig`: MicroservicesConfig
- Add getters/setters

### 1.6 Add `frontendConfig` to `ProjectRequest`
**Modify**: `backend/src/main/java/com/firas/generator/model/ProjectRequest.java`
- Add `private FrontendConfig frontendConfig;`
- Add getter/setter
- Add `getEffectiveFrontendConfig()` helper (returns frontendConfig or new FrontendConfig())

---

## PHASE 2: Microservices Backend Generation

### 2.1 Create `MicroservicesGenerator` service
**New file**: `backend/src/main/java/com/firas/generator/stack/spring/MicroservicesGenerator.java`

A `@Component` that SpringStackProvider delegates to when `architectureType == MICROSERVICES`.

Key methods:
- `computeServiceDefinitions(ProjectRequest)` - AUTO mode: 1 service per non-join table; MANUAL mode: uses serviceTableMapping
- `generateMicroservicesProject(ProjectRequest)` - Returns `List<FilePreview>` with all files:
  1. Parent POM (multi-module with `<modules>`)
  2. Discovery Server module (Eureka Server)
  3. Config Server module (Spring Cloud Config)
  4. API Gateway module (Spring Cloud Gateway with routes for each service)
  5. Per-service modules (reuses existing SpringCodeGenerator for CRUD, prefixes paths with service name)
  6. Docker-compose with all services (if includeDocker)
  7. Root .gitignore

**Per-service module generation**:
- Creates a scoped clone of ProjectRequest with only this service's tables
- Calls existing `SpringCodeGenerator.generateEntity/Repository/Service/Controller/Dto/Mapper` methods
- Prepends `serviceName + "/"` to each FilePreview path
- Generates service-specific pom.xml (with eureka-client, openfeign dependencies)
- Generates service-specific application.yml (eureka registration, port, DB)
- Generates service Application.java with `@EnableDiscoveryClient @EnableFeignClients`
- Generates `@FeignClient` interfaces for cross-service relationships (when table A in service X references table B in service Y)

### 2.2 Modify `SpringStackProvider.generateProject()`
**Modify**: `backend/src/main/java/com/firas/generator/stack/spring/SpringStackProvider.java`

- Inject `MicroservicesGenerator` via constructor
- At the top of `generateProject()`, after `applyTypeMappings()`:
  ```java
  if (springConfig.getArchitectureType() == ArchitectureType.MICROSERVICES) {
      return microservicesGenerator.generateMicroservicesProject(request);
  }
  // ... existing monolith logic unchanged
  ```

### 2.3 Create Microservices FreeMarker templates
**New directory**: `backend/src/main/resources/templates/spring/microservices/`

| Template | Purpose |
|---|---|
| `parent-pom.xml.ftl` | Multi-module parent POM with Spring Cloud BOM, `<modules>` section |
| `discovery-server-pom.xml.ftl` | Eureka Server pom.xml |
| `discovery-server-Application.java.ftl` | `@EnableEurekaServer` main class |
| `discovery-server-application.yml.ftl` | Eureka server config (port, self-register=false) |
| `config-server-pom.xml.ftl` | Config Server pom.xml |
| `config-server-Application.java.ftl` | `@EnableConfigServer` main class |
| `config-server-application.yml.ftl` | Config server config (port, native profile) |
| `api-gateway-pom.xml.ftl` | Gateway pom.xml with spring-cloud-gateway |
| `api-gateway-Application.java.ftl` | `@EnableDiscoveryClient` main class |
| `api-gateway-application.yml.ftl` | Route definitions for each microservice |
| `service-pom.xml.ftl` | Per-service pom.xml (eureka-client, feign, web, jpa, driver) |
| `service-application.yml.ftl` | Per-service config (eureka registration, port, DB) |
| `service-Application.java.ftl` | `@EnableDiscoveryClient @EnableFeignClients` main class |
| `FeignClient.ftl` | `@FeignClient` interface for inter-service calls |
| `docker-compose-microservices.yml.ftl` | Docker compose with all services + supporting systems |

---

## PHASE 3: Frontend Generation Backend (Factory Pattern)

### 3.1 Create `FrontendProvider` interface
**New file**: `backend/src/main/java/com/firas/generator/frontend/FrontendProvider.java`
```java
public interface FrontendProvider {
    String getFramework();  // "NEXTJS", "ANGULAR", "REACT"
    List<FilePreview> generateFrontend(ProjectRequest request) throws IOException;
    boolean isAvailable();
}
```

### 3.2 Create `FrontendProviderFactory`
**New file**: `backend/src/main/java/com/firas/generator/frontend/FrontendProviderFactory.java`
- `@Component`, auto-discovers all `FrontendProvider` beans via Spring DI
- `getProvider(String framework)`, `getAvailableFrameworks()`, `hasProvider(String framework)`

### 3.3 Create `NextJsFrontendProvider`
**New file**: `backend/src/main/java/com/firas/generator/frontend/nextjs/NextJsFrontendProvider.java`

`@Component` implementing `FrontendProvider`. All generated files are prefixed with `frontend/`.

Generation steps:
1. **Config files**: package.json, tsconfig.json, tailwind.config.ts, next.config.ts, postcss.config.mjs, .gitignore, .env.local
2. **App skeleton**: app/layout.tsx, app/page.tsx, app/globals.css
3. **API client**: lib/api.ts (fetch wrapper with GET/POST/PUT/DELETE, auth token injection)
4. **TypeScript types**: types/index.ts (interfaces generated from Table/Column definitions)
5. **UI components**: components/ui/button.tsx, table.tsx, form-field.tsx, modal.tsx
6. **Navigation**: components/navbar.tsx
7. **Per-entity CRUD pages** (for each non-join table):
   - `app/[entity]/page.tsx` - List with search + pagination + delete
   - `app/[entity]/new/page.tsx` - Create form
   - `app/[entity]/[id]/page.tsx` - Detail view
   - `app/[entity]/[id]/edit/page.tsx` - Edit form
8. **Auth pages** (if security enabled):
   - `app/login/page.tsx` - Login form
   - `app/register/page.tsx` - Registration form
   - `lib/auth.ts` - Auth context with token storage, login/logout/register functions

### 3.4 Create Next.js FreeMarker templates
**New directory**: `backend/src/main/resources/templates/frontend/nextjs/`

| Template | Purpose |
|---|---|
| `package.json.ftl` | Dependencies: next, react, tailwindcss, etc. |
| `tsconfig.json.ftl` | TypeScript config with path aliases |
| `tailwind.config.ts.ftl` | Tailwind content paths |
| `next.config.ts.ftl` | API proxy rewrites to backend |
| `postcss.config.mjs.ftl` | PostCSS with tailwindcss plugin |
| `.gitignore.ftl` | Next.js gitignore |
| `.env.local.ftl` | `NEXT_PUBLIC_API_URL` pointing to backend |
| `globals.css.ftl` | Tailwind base/components/utilities imports |
| `layout.tsx.ftl` | Root layout with `<Navbar />` and html/body |
| `page.tsx.ftl` | Home page with entity links dashboard |
| `api.ts.ftl` | Generic fetch client: `apiGet`, `apiPost`, `apiPut`, `apiDelete` |
| `auth.ts.ftl` | Auth context: `AuthProvider`, `useAuth`, `login()`, `logout()`, `register()` |
| `types.ts.ftl` | TS interfaces from tables: `<#list tables as table>export interface ${table.className} { ... }` |
| `navbar.tsx.ftl` | Nav bar with links to each entity + auth links |
| `button.tsx.ftl` | Reusable button component |
| `data-table.tsx.ftl` | Generic data table component |
| `form-field.tsx.ftl` | Generic form field (text, number, boolean, date, select) |
| `modal.tsx.ftl` | Confirmation modal for delete |
| `entity-list-page.tsx.ftl` | List page: fetch all, paginated table, delete button |
| `entity-create-page.tsx.ftl` | Create page: form with validation |
| `entity-detail-page.tsx.ftl` | Detail page: fetch by ID, display all fields |
| `entity-edit-page.tsx.ftl` | Edit page: prefilled form, submit PUT |
| `login-page.tsx.ftl` | Login form → POST /api/auth/login |
| `register-page.tsx.ftl` | Register form → POST /api/auth/register |

### 3.5 Modify `GeneratorController` to integrate frontend generation
**Modify**: `backend/src/main/java/com/firas/generator/controller/GeneratorController.java`

- Inject `FrontendProviderFactory`
- Change `generateProject()` endpoint:
  ```java
  // Get backend files
  List<FilePreview> allFiles = new ArrayList<>(provider.generateProject(request));

  // Append frontend files if enabled
  FrontendConfig fc = request.getEffectiveFrontendConfig();
  if (fc.isEnabled() && frontendProviderFactory.hasProvider(fc.getFramework())) {
      allFiles.addAll(frontendProviderFactory.getProvider(fc.getFramework()).generateFrontend(request));
  }

  // Create ZIP from combined files
  byte[] zipContent = ZipUtils.createZipFromFilePreviews(allFiles, filename);
  ```
- Change `previewProject()` similarly to include frontend files in preview response

---

## PHASE 4: Frontend UI Changes

### 4.1 Update Zustand store types
**Modify**: `frontend/lib/store.ts`

Add new types:
```typescript
export type ArchitectureType = "MONOLITH" | "MICROSERVICES"
export type MicroservicesMode = "AUTO" | "MANUAL"

export interface MicroservicesConfig {
  mode: MicroservicesMode
  serviceTableMapping: Record<string, string[]>
  discoveryPort: number
  configPort: number
  gatewayPort: number
  serviceStartPort: number
}

export type FrontendFramework = "NEXTJS" | "ANGULAR" | "REACT"

export interface FrontendConfig {
  enabled: boolean
  framework: FrontendFramework
  port: number
  backendUrl: string
}
```

Update `SpringConfig` to add:
- `architectureType: ArchitectureType`
- `microservicesConfig: MicroservicesConfig`

Update `ProjectConfig` to add:
- `frontendConfig: FrontendConfig`

Add defaults:
- `defaultSpringConfig.architectureType = "MONOLITH"`
- `defaultSpringConfig.microservicesConfig = { mode: "AUTO", serviceTableMapping: {}, discoveryPort: 8761, configPort: 8888, gatewayPort: 8080, serviceStartPort: 8081 }`
- `defaultFrontendConfig = { enabled: false, framework: "NEXTJS", port: 3000, backendUrl: "http://localhost:8080" }`

Add actions: `setFrontendConfig`, `setMicroservicesConfig`

### 4.2 Update `project-config-phase.tsx` - Architecture Selector
**Modify**: `frontend/components/generator/project-config-phase.tsx`

In the Spring config section, add:

**Architecture Type** - Monolith/Microservices toggle (segmented control or radio). Shown only when `stackType === "SPRING"`.

When MICROSERVICES selected, show:
- **Mode toggle**: Auto / Manual
- **Auto mode**: Read-only preview list showing "Each entity becomes its own service: user-service, post-service, ..."
- **Manual mode**: Service grouping UI:
  - "Add Service" button to create named service groups
  - Each group has a name input and multi-select checkboxes for available tables
  - Tables already assigned to another service are disabled
- **Ports config** (collapsible): discovery, config, gateway, starting service port

### 4.3 Update `project-config-phase.tsx` - Frontend Generation Toggle
**Modify**: `frontend/components/generator/project-config-phase.tsx`

Add section (after Dependencies, before Generate buttons):
- **"Generate Frontend" toggle** (Switch component)
- When enabled:
  - Framework selector: "Next.js" (active), "Angular (coming soon)", "React (coming soon)" -- grayed out options
  - Brief description text
- Invisible when disabled to keep UI clean

### 4.4 Update `getProjectPayload()` in `project-config-phase.tsx`

Include new fields in the payload sent to backend:
- `springConfig.architectureType`
- `springConfig.microservicesConfig` (only when MICROSERVICES)
- `frontendConfig` (always, defaults to `{ enabled: false }`)

---

## PHASE 5: Documentation Updates

### 5.1 Update `DOCUMENTATION_MAP.md`
Add sections for:
- Microservices Architecture documentation
- Frontend Generation documentation

### 5.2 Update `backend/Documentation.md`
Add:
- MicroservicesConfig model documentation
- MicroservicesGenerator service documentation
- FrontendProvider interface + NextJsFrontendProvider documentation
- Updated API reference (new fields in ProjectRequest)
- Updated design patterns table (add Frontend Factory)

### 5.3 Update `frontend/Documentation.md`
Add:
- New store types (ArchitectureType, MicroservicesConfig, FrontendConfig)
- New UI sections in project-config-phase
- Updated payload structure

### 5.4 Update `backend/src/main/java/com/firas/generator/stack/spring/documentation.md`
Add:
- Microservices template structure
- Generation flow for microservices mode
- Per-service code generation details

### 5.5 Create `backend/src/main/java/com/firas/generator/frontend/documentation.md`
New documentation for:
- FrontendProvider interface
- NextJsFrontendProvider implementation
- Template system overview for Next.js

---

## PHASE 6: Test & Verification Updates

### 6.1 Update `temp_request.json`
Add new fields to the existing sample request:
```json
{
  "springConfig": {
    ...existing...,
    "architectureType": "MONOLITH"
  },
  "frontendConfig": {
    "enabled": true,
    "framework": "NEXTJS",
    "port": 3000,
    "backendUrl": "http://localhost:8080"
  }
}
```

Also create `temp_request_microservices.json` - a separate request file for microservices testing:
```json
{
  "springConfig": {
    "architectureType": "MICROSERVICES",
    "microservicesConfig": {
      "mode": "MANUAL",
      "serviceTableMapping": {
        "user-service": ["users"],
        "content-service": ["posts", "comments"]
      }
    }
  },
  "frontendConfig": { "enabled": true, "framework": "NEXTJS" }
}
```

### 6.2 Update `verify_generation.ps1`
Add new test phases:

**After existing Phase 2** (generate monolith):
- Phase 2B: Generate microservices project using `temp_request_microservices.json`
- Phase 3B: Extract and verify multi-module structure:
  - Check parent pom.xml exists and contains `<modules>`
  - Check discovery-server/ directory and its pom.xml
  - Check config-server/ directory and its pom.xml
  - Check api-gateway/ directory and its pom.xml
  - Check service directories (user-service/, content-service/)
- Phase 4B: Build parent POM (`mvn clean compile`)

**After existing Phase 7** (E2E tests):
- Phase 7B: Verify frontend files:
  - Check `frontend/package.json` exists
  - Check `frontend/app/layout.tsx` exists
  - Check `frontend/types/index.ts` exists
  - Check entity CRUD pages exist (e.g., `frontend/app/users/page.tsx`)
  - If security enabled: Check `frontend/app/login/page.tsx` exists

---

## Critical Files Summary

### Files to CREATE (new):
| File | Purpose |
|---|---|
| `backend/.../model/config/ArchitectureType.java` | Enum: MONOLITH, MICROSERVICES |
| `backend/.../model/config/MicroservicesConfig.java` | Microservices configuration |
| `backend/.../model/config/ServiceDefinition.java` | Internal service representation |
| `backend/.../model/config/FrontendConfig.java` | Frontend generation config |
| `backend/.../stack/spring/MicroservicesGenerator.java` | Microservices orchestrator |
| `backend/.../frontend/FrontendProvider.java` | Frontend generation interface |
| `backend/.../frontend/FrontendProviderFactory.java` | Factory for frontend providers |
| `backend/.../frontend/nextjs/NextJsFrontendProvider.java` | Next.js implementation |
| `backend/src/main/resources/templates/spring/microservices/*.ftl` | ~15 templates |
| `backend/src/main/resources/templates/frontend/nextjs/*.ftl` | ~22 templates |
| `temp_request_microservices.json` | Microservices test request |
| `backend/.../frontend/documentation.md` | Frontend generation docs |

### Files to MODIFY (existing):
| File | Change |
|---|---|
| `backend/.../model/config/SpringConfig.java` | Add `architectureType`, `microservicesConfig` |
| `backend/.../model/ProjectRequest.java` | Add `frontendConfig` field + helper |
| `backend/.../stack/spring/SpringStackProvider.java` | Branch on architectureType, inject MicroservicesGenerator |
| `backend/.../controller/GeneratorController.java` | Integrate FrontendProvider, change ZIP creation flow |
| `frontend/lib/store.ts` | New types, defaults, and actions |
| `frontend/components/generator/project-config-phase.tsx` | Architecture selector, microservices grouping UI, frontend toggle |
| `DOCUMENTATION_MAP.md` | Add new sections |
| `backend/Documentation.md` | Document new components |
| `frontend/Documentation.md` | Document new UI/types |
| `backend/.../stack/spring/documentation.md` | Microservices generation |
| `temp_request.json` | Add frontendConfig |
| `verify_generation.ps1` | Add microservices and frontend verification phases |

---

## Verification Plan

### Manual Testing:
1. Start backend: `mvn -f backend/pom.xml spring-boot:run`
2. Test monolith generation (existing temp_request.json) - verify nothing is broken
3. Test monolith + frontend: Send request with `frontendConfig.enabled=true` - verify frontend/ directory in ZIP
4. Test microservices: Send temp_request_microservices.json - verify multi-module structure
5. Test microservices + frontend: Verify combined output
6. Start frontend dev server, verify UI shows architecture toggle and frontend toggle
7. End-to-end: Use the UI to generate a microservices project with frontend

### Automated Testing:
Run `verify_generation.ps1` which will test:
- Monolith generation, build, test, run, E2E
- Microservices generation, structure verification, build
- Frontend file existence verification

### Key Backward Compatibility Checks:
- Requests without `architectureType` default to MONOLITH (existing behavior)
- Requests without `frontendConfig` default to `enabled: false` (no frontend generated)
- All existing endpoints remain unchanged
- Existing frontend UI works identically until user interacts with new toggles
