# Frontend Generation - Implementation Details

This document details the **Frontend Generation** feature, which generates complete frontend projects from the same table/column metadata used for backend generation.

## Architecture

### FrontendProvider Interface

The `FrontendProvider` interface follows the Strategy pattern, allowing pluggable frontend framework generators:

```java
public interface FrontendProvider {
    String getFramework();          // "NEXTJS", "ANGULAR", "REACT"
    List<FilePreview> generateFrontend(ProjectRequest request) throws IOException;
    boolean isAvailable();
}
```

### FrontendProviderFactory

The `FrontendProviderFactory` auto-discovers all `FrontendProvider` beans via Spring DI and provides:
- `getProvider(String framework)` - Get a specific provider
- `hasProvider(String framework)` - Check if a provider exists
- `getAvailableFrameworks()` - List all available frameworks

### Integration with GeneratorController

The `GeneratorController` appends frontend files when `frontendConfig.enabled == true`:

```java
FrontendConfig fc = request.getEffectiveFrontendConfig();
if (fc.isEnabled() && frontendProviderFactory.hasProvider(fc.getFramework())) {
    allFiles.addAll(frontendProviderFactory.getProvider(fc.getFramework()).generateFrontend(request));
}
```

## NextJsFrontendProvider

The `NextJsFrontendProvider` generates a complete Next.js 14 frontend with:

### Generated File Structure

```
frontend/
├── package.json                    # Dependencies (Next.js, React, Tailwind CSS)
├── tsconfig.json                   # TypeScript config with path aliases
├── tailwind.config.ts              # Tailwind content paths
├── next.config.ts                  # API proxy rewrites
├── postcss.config.mjs              # PostCSS config
├── .gitignore                      # Next.js gitignore
├── .env.local                      # NEXT_PUBLIC_API_URL
├── app/
│   ├── layout.tsx                  # Root layout with Navbar
│   ├── page.tsx                    # Dashboard with entity links
│   ├── globals.css                 # Tailwind imports + base styles
│   ├── login/page.tsx              # Login form (if security enabled)
│   ├── register/page.tsx           # Register form (if security enabled)
│   └── [entity]/                   # Per-entity CRUD pages
│       ├── page.tsx                # List with search, pagination, delete
│       ├── new/page.tsx            # Create form
│       └── [id]/
│           ├── page.tsx            # Detail view
│           └── edit/page.tsx       # Edit form
├── lib/
│   ├── api.ts                      # Fetch client (GET/POST/PUT/DELETE + auth)
│   └── auth.ts                     # Auth context (if security enabled)
├── types/
│   └── index.ts                    # TypeScript interfaces from tables
└── components/
    ├── navbar.tsx                   # Navigation with entity + auth links
    └── ui/
        ├── button.tsx              # Reusable button component
        ├── data-table.tsx          # Generic data table
        ├── form-field.tsx          # Form field (text, number, boolean, date)
        └── modal.tsx               # Confirmation dialog
```

### Type Mapping (Java → TypeScript)

| Java Type | TypeScript Type |
| :--- | :--- |
| `String` | `string` |
| `Long`, `Integer`, `int` | `number` |
| `Double`, `Float`, `BigDecimal` | `number` |
| `Boolean`, `boolean` | `boolean` |
| `LocalDateTime`, `Date`, `Timestamp` | `string` |
| Other | `any` |

### Template System

Templates are located in `templates/frontend/nextjs/` and use FreeMarker syntax with model variables:
- `projectName` - Project name
- `tables` - Non-join tables
- `allTables` - All tables
- `backendUrl` - Backend API URL
- `port` - Frontend dev server port
- `hasSecurity` / `hasJwt` - Security flags
- `table` / `entity` - Current table (for per-entity templates)

### FrontendConfig Model

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `enabled` | boolean | `false` | Whether to generate frontend |
| `framework` | String | `"NEXTJS"` | Framework to use |
| `port` | int | `3000` | Dev server port |
| `backendUrl` | String | `"http://localhost:8080"` | Backend API URL |

## Adding New Frontend Frameworks

To add support for a new framework (e.g., Angular):

1. Create `AngularFrontendProvider` implementing `FrontendProvider`
2. Annotate with `@Component`
3. Return `"ANGULAR"` from `getFramework()`
4. Create templates in `templates/frontend/angular/`
5. The `FrontendProviderFactory` will auto-discover it
