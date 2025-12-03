# Spring Generator - Frontend Documentation

## Table of Contents
1. [Overview](#overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Getting Started](#getting-started)
5. [Core Features](#core-features)
6. [API Integration](#api-integration)
7. [State Management](#state-management)
8. [Components Overview](#components-overview)
9. [Styling & Design](#styling--design)
10. [Development Guide](#development-guide)
11. [Deployment](#deployment)

---

## Overview

The Spring Generator frontend is a modern, premium web application built with **Next.js 15** and **React 19**. It provides a visual, interactive interface for generating Spring Boot projects from SQL schemas.

### Key Highlights
- **3-Phase Wizard**: SQL parsing → Visual schema editing → Project configuration
- **Real-time Visual Editor**: Drag-and-drop ER diagram builder using React Flow
- **AI-Powered Features**: AI schema generation modal
- **Premium UI/UX**: Glassmorphism, gradients, smooth animations with Framer Motion
- **Full Backend Integration**: Seamless API calls to Spring Boot backend

---

## Technology Stack

### Core Framework
- **Next.js 15.1.3** - React framework with App Router
- **React 19.0.0** - UI library
- **TypeScript** - Type safety

### UI & Styling
- **Tailwind CSS 4** - Utility-first CSS framework
- **TW Animate CSS** - Extended animations
- **Radix UI** - Headless UI components
- **Framer Motion** - Animation library
- **Lucide React** - Icon library

### State & Data Flow
- **Zustand 5.0.2** - Lightweight state management
- **React Flow 11.11.4** - Visual node-based editor

### Form & Validation
- **React Hook Form** - Form management
- **Zod** - Schema validation

### Utilities
- **Sonner** - Toast notifications
- **Class Variance Authority** - Component variants
- **clsx** & **tailwind-merge** - Conditional class utilities

---

## Project Structure

```
frontend/
├── app/                          # Next.js App Router
│   ├── page.tsx                  # Landing page
│   ├── layout.tsx                # Root layout
│   ├── globals.css               # Global styles
│   ├── contact/page.tsx          # Contact form
│   ├── pricing/page.tsx          # Pricing tiers
│   └── generator/page.tsx        # Generator wizard
├── components/
│   ├── ui/                       # Reusable UI components (Radix-based)
│   │   ├── button.tsx
│   │   ├── input.tsx
│   │   ├── card.tsx
│   │   ├── dialog.tsx
│   │   └── ... (40+ components)
│   ├── landing/                  # Landing page sections
│   │   ├── hero.tsx
│   │   ├── features.tsx
│   │   ├── how-it-works.tsx
│   │   └── cta.tsx
│   ├── generator/                # Core generator features
│   │   ├── phase-indicator.tsx   # Progress stepper
│   │   ├── sql-parser-phase.tsx  # Phase 1: SQL input
│   │   ├── schema-editor-phase.tsx # Phase 2: Visual editor
│   │   ├── project-config-phase.tsx # Phase 3: Configuration
│   │   ├── table-node.tsx        # React Flow table node
│   │   ├── table-editor.tsx      # Table editing modal
│   │   ├── dependencies-modal.tsx # Browse/select dependencies
│   │   └── ai-generate-modal.tsx # AI schema generation
│   ├── navbar.tsx                # Top navigation
│   └── footer.tsx                # Site footer
├── lib/
│   ├── store.ts                  # Zustand state management
│   └── utils.ts                  # Utility functions
├── hooks/                        # Custom React hooks
│   ├── use-toast.ts
│   └── use-mobile.ts
├── public/                       # Static assets
└── styles/                       # Additional styles

```

---

## Getting Started

### Prerequisites
- **Node.js** 18.17+ or 20+
- **npm**, **yarn**, or **pnpm**
- Backend running on `http://localhost:8080`

### Installation

```bash
# Clone the repository
git clone <repository-url>
cd springInitializer/frontend

# Install dependencies
npm install

# Create environment file
cp .env.example .env.local

# Start development server
npm run dev
```

### Environment Variables

Create a `.env.local` file:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

### Development Commands

```bash
npm run dev          # Start dev server (http://localhost:3000)
npm run build        # Build for production
npm run start        # Start production server
npm run lint         # Run ESLint
npm run type-check   # TypeScript type checking
```

---

## Core Features

### 1. **Landing Page**
- Hero section with gradient text effects
- Feature showcase (6 key features)
- How It Works section
- Call-to-action sections
- Responsive design

### 2. **Contact Page**
- Premium contact form
- Contact information cards
- Form validation
- Toast notifications

### 3. **Pricing Page**
- 3-tier pricing model
- Feature comparison
- Popular plan highlighting
- Animated cards

### 4. **Generator Wizard (Main Feature)**

#### **Phase 1: SQL Parser**
- SQL code editor with syntax highlighting
- Sample SQL loader
- Backend API integration with fallback
- Error handling and validation
- Supports: `CREATE TABLE`, `ALTER TABLE`, `FOREIGN KEY`

#### **Phase 2: Visual Schema Editor**
- React Flow-based ER diagram
- Drag-and-drop table positioning
- Visual relationship lines
- Table editing modal:
  - Add/remove columns
  - Edit column properties (name, type, constraints)
  - Set primary keys, foreign keys, unique constraints
- AI schema generation modal
- Undo/Redo functionality
- Zoom and pan controls

#### **Phase 3: Project Configuration**
- Project metadata form:
  - Group ID, Artifact ID, Name, Description
  - Package Name
  - Java Version (17, 21)
  - Spring Boot Version (3.2.0, 3.1.5, 3.0.12)
- Dependencies modal:
  - Categorized by groups (Web, Security, SQL, NoSQL, etc.)
  - Search functionality
  - Visual category badges
- Generation summary
- Generation summary
- Download ZIP file

#### **Phase 4: IDE Preview (New)**
- **CodePreviewModal**: Full-screen IDE-like interface
- **File Tree**: Navigate through generated project structure
- **Monaco Editor**: View and edit files with syntax highlighting
- **Live Editing**: Modify code before downloading
- **Download**: Generate ZIP with your custom changes

---

## API Integration

The frontend communicates with the Spring Boot backend via REST APIs.

### API Endpoints

#### 1. **Parse SQL**
```typescript
GET /api/sqlParser/{url-encoded-sql}

Response: Table[]
```

**Frontend Implementation:** `sql-parser-phase.tsx` (line 278)

```typescript
const encodedSql = encodeURIComponent(sqlInput)
const response = await fetch(
  `http://localhost:8080/api/sqlParser/${encodedSql}`
)
const tables = await response.json()
```

**Response Format:**
```json
[
  {
    "name": "users",
    "className": "User",
    "columns": [
      {
        "name": "id",
        "fieldName": "id",
        "javaType": "Long",
        "type": "BIGINT",
        "primaryKey": true,
        "autoIncrement": true,
        "nullable": false,
        "unique": false,
        "foreignKey": false
      }
    ],
    "relationships": [],
    "isJoinTable": false
  }
]
```

#### 2. **Generate Project**
```typescript
POST /api/generate/project
Content-Type: application/json

Body: ProjectGenerationPayload
Response: application/zip
```

**Frontend Implementation:** `project-config-phase.tsx` (line 137)

```typescript
const payload = {
  groupId: "com.example",
  artifactId: "demo",
  name: "Demo",
  description: "Demo project",
  packageName: "com.example.demo",
  javaVersion: "17",
  bootVersion: "3.2.0",
  dependencies: [...],  // Array of full Dependency objects (converted from IDs)
  includeEntity: true,
  includeRepository: true,
  includeService: true,
  includeController: true,
  includeDto: false,
  includeMapper: false,
  tables: [...]         // Array of table objects
}

const response = await fetch(
  'http://localhost:8080/api/generate/project',
  {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }
)

const blob = await response.blob()
// Trigger download...
```

### Fallback Mechanism

Both endpoints implement graceful fallbacks:
- **SQL Parser**: Falls back to client-side mock parser if backend unavailable
- **Project Generator**: Downloads a text specification file if backend unavailable

---

## State Management

### Zustand Store (`lib/store.ts`)

The entire generator state is managed in a single Zustand store:

```typescript
interface GeneratorStore {
  // Phase navigation
  currentPhase: number
  setCurrentPhase: (phase: number) => void
  
  // SQL input
  sqlInput: string
  setSqlInput: (sql: string) => void
  
  // Parsed tables (editable)
  tables: Table[]
  setTables: (tables: Table[]) => void
  addTable: (table: Table) => void
  updateTable: (id: string, updates: Partial<Table>) => void
  deleteTable: (id: string) => void
  
  // History (undo/redo)
  history: Table[][]
  historyIndex: number
  undo: () => void
  redo: () => void
  
  // Project configuration
  projectConfig: ProjectConfig
  setProjectConfig: (config: Partial<ProjectConfig>) => void
  
  // Loading states
  isParsing: boolean
  isGenerating: boolean
  
  // Reset
  reset: () => void
}
```

### Data Models

#### **Table**
```typescript
interface Table {
  id: string
  name: string
  className: string
  columns: Column[]
  relationships: Relationship[]
  isJoinTable: boolean
  position?: { x: number; y: number }
}
```

#### **Column**
```typescript
interface Column {
  name: string
  type: string  // SQL type
  fieldName: string
  javaType: string
  primaryKey: boolean
  autoIncrement: boolean
  nullable: boolean
  foreignKey: boolean
  referencedTable?: string
  referencedColumn?: string
  unique: boolean
  length?: number
}
```

#### **Relationship**
```typescript
interface Relationship {
  type: "ONE_TO_ONE" | "ONE_TO_MANY" | "MANY_TO_ONE" | "MANY_TO_MANY"
  sourceTable: string
  targetTable: string
  sourceColumn?: string
  targetColumn?: string
  joinTable?: string
  mappedBy?: string
  fieldName: string
  targetClassName?: string
}
```

#### **ProjectConfig**
```typescript
interface ProjectConfig {
  groupId: string
  artifactId: string
  name: string
  description: string
  packageName: string
  javaVersion: string
  bootVersion: string
  dependencies: string[] // Stores dependency IDs, converted to full objects before API call
  
  // Code generation flags
  includeEntity: boolean
  includeRepository: boolean
  includeService: boolean
  includeController: boolean
  includeDto: boolean
  includeMapper: boolean
}
```

---

## Components Overview

### UI Components (`components/ui/`)

All UI components are based on **Radix UI** primitives with custom styling:

- **Forms**: `button`, `input`, `textarea`, `select`, `checkbox`, `radio-group`
- **Layout**: `card`, `dialog`, `drawer`, `sheet`, `tabs`, `accordion`
- **Feedback**: `alert`, `toast`, `popover`, `tooltip`, `hover-card`
- **Data**: `table`, `dropdown-menu`, `context-menu`, `command`
- **Advanced**: `carousel`, `chart`, `calendar`, `date-picker`

### Generator Components

#### **1. PhaseIndicator** 
Progress stepper showing current phase (1,  2, or 3).

#### **2. SqlParserPhase**
- Code editor with monaco-like styling
- "Load Sample" button
- Error display
- Parse button with loading state

#### **3. SchemaEditorPhase**
- React Flow canvas
- Custom table nodes
- Relationship edges
- Controls (zoom, fit view)
- Toolbar:
  - Add Table
  - AI Generate
  - Undo/Redo
- Table editing modal

#### **4. TableEditor**
Modal for editing table properties:
- Table name
- Columns (add, remove, edit)
- Column properties:
  - Name, Java type, SQL type
  - Constraints (PK, FK, Unique, Nullable, Auto-increment)
  - Default values

#### **5. DependenciesModal**
Searchable, categorized dependency browser:
- Groups: Web, Security, SQL, NoSQL, Messaging, I/O, Ops, Testing, Developer Tools
- Search bar
- Checkbox selection
- Category badges with colors

#### **6. AiGenerateModal**
AI-powered schema generation:
- Text input for description
- "Generate Schema" button
- Mock implementation (ready for AI integration)

#### **7. ProjectConfigPhase**
- Metadata form
- Dependencies summary with remove buttons
- Generation summary (entities, fields, dependencies count)
- "Generate Project" button with glow animation
- Success screen

---

## Styling & Design

### Design System

The app uses a **dark mode, futuristic** design system:

**Colors** (OKLCH color space):
- **Primary**: Cyan (`oklch(0.7 0.18 200)`)
- **Accent**: Purple (`oklch(0.65 0.25 300)`)
- **Background**: Dark purple (`oklch(0.08 0.01 260)`)
- **Card**: Slightly lighter (`oklch(0.12 0.01 260)`)

**Effects**:
- **Glassmorphism**: `glass` utility class
- **Gradients**: `gradient-text`, gradient backgrounds
- **Glow**: `glow`, `glow-sm` utility classes
- **Animations**: Framer Motion for page transitions, hover effects

### Custom Utility Classes

```css
.glass {
  @apply bg-card/40 backdrop-blur-xl border border-border/50;
}

.glass-strong {
  @apply bg-card/60 backdrop-blur-2xl border border-border/60;
}

.gradient-text {
  @apply bg-gradient-to-r from-primary via-accent to-primary 
         bg-clip-text text-transparent;
}

.glow {
  box-shadow: 0 0 40px oklch(0.7 0.18 200 / 0.3),
              0 0 80px oklch(0.65 0.25 300 / 0.2);
}
```

### Responsive Design

- Mobile-first approach
- Breakpoints: `sm` (640px), `md` (768px), `lg` (1024px), `xl` (1280px)
- Touch-friendly UI elements
- Responsive grid layouts

---

## Development Guide

### Adding a New Page

1. Create file in `app/` directory:
```tsx
// app/new-page/page.tsx
import { Navbar } from "@/components/navbar"
import { Footer } from "@/components/footer"

export default function NewPage() {
  return (
    <main className="min-h-screen">
      <Navbar />
      {/* Your content */}
      <Footer />
    </main>
  )
}
```

2. Update navigation in `navbar.tsx`

### Adding a New UI Component

Use the Radix UI + CVA pattern:

```tsx
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const myComponentVariants = cva(
  "base-classes",
  {
    variants: {
      variant: {
        default: "...",
        destructive: "...",
      },
      size: {
        default: "h-9",
        lg: "h-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

export function MyComponent({
  className,
  variant,
  size,
  ...props
}: React.ComponentProps<'div'> & VariantProps<typeof myComponentVariants>) {
  return (
    <div
      className={cn(myComponentVariants({ variant, size }), className)}
      {...props}
    />
  )
}
```

### Extending the Store

Add new state to `lib/store.ts`:

```typescript
interface GeneratorStore {
  // ... existing state
  
  // New state
  myNewState: string
  setMyNewState: (value: string) => void
}

export const useGeneratorStore = create<GeneratorStore>((set) => ({
  // ... existing implementation
  
  // New implementation
  myNewState: "",
  setMyNewState: (value) => set({ myNewState: value }),
}))
```

### API Integration Best Practices

1. **Use try-catch with fallbacks**:
```typescript
try {
  const response = await fetch(API_URL)
  if (response.ok) {
    // Handle success
  } else {
    throw new Error()
  }
} catch {
  // Fallback logic
}
```

2. **Use AbortSignal for timeouts**:
```typescript
fetch(url, {
  signal: AbortSignal.timeout(5000)
})
```

3. **Show loading states**:
```typescript
setIsParsing(true)
try {
  // API call
} finally {
  setIsParsing(false)
}
```

4. **Toast notifications**:
```typescript
toast.success("Success message")
toast.error("Error message")
toast.info("Info message", {
  description: "Additional details"
})
```

---

## Deployment

### Build for Production

```bash
npm run build
```

This creates an optimized production build in `.next/`

### Environment Variables

For production, set:
```env
NEXT_PUBLIC_API_URL=https://your-backend-api.com/api
```

### Deployment Platforms

#### **Vercel** (Recommended)
```bash
npm i -g vercel
vercel
```

#### **Docker**
```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "start"]
```

#### **Static Export** (if no server features needed)
```javascript
// next.config.mjs
const nextConfig = {
  output: 'export',
};
```

Then:
```bash
npm run build
# Outputs to 'out/' directory
```

---

## Troubleshooting

### Common Issues

**1. API Connection Failed**
- Ensure backend is running on `http://localhost:8080`
- Check CORS configuration in backend
- Verify `NEXT_PUBLIC_API_URL` environment variable

**2. Build Errors**
- Clear `.next` folder: `rm -rf .next`
- Delete `node_modules` and reinstall: `rm -rf node_modules && npm install`
- Check TypeScript errors: `npm run type-check`

**3. State Not Persisting**
- Zustand state is in-memory only
- To persist, add `persist` middleware from `zustand/middleware`

**4. Styling Issues**
- Ensure Tailwind CSS is configured correctly
- Check `postcss.config.mjs` and `tailwind.config.ts`
- Clear browser cache

---

## Performance Optimization

### Recommendations

1. **Code Splitting**: Next.js does this automatically with App Router
2. **Image Optimization**: Use `next/image` component
3. **Lazy Loading**: Use `React.lazy()` for heavy components
4. **Memoization**: Use `React.memo()`, `useMemo()`, `useCallback()` for expensive operations

### Current Optimizations

- Server Components where possible
- Dynamic imports for modals
- Optimized SVG icons from Lucide
- Minimal JavaScript bundle with tree-shaking

---

## Future Enhancements

### Planned Features
- [ ] Real AI schema generation integration (OpenAI/Gemini API)
- [ ] User authentication and project saving
- [ ] Project templates library
- [ ] Export diagrams as PNG/SVG
- [ ] Collaborative editing (multiplayer)
- [ ] Dark/Light mode toggle
- [ ] Internationalization (i18n)
- [ ] Accessibility improvements (WCAG 2.1 AA)

### Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

---

## Resources

- [Next.js Documentation](https://nextjs.org/docs)
- [Tailwind CSS](https://tailwindcss.com/docs)
- [Radix UI](https://www.radix-ui.com/)
- [Zustand](https://zustand-demo.pmnd.rs/)
- [React Flow](https://reactflow.dev/)
- [Framer Motion](https://www.framer.com/motion/)

---

**Last Updated**: 2025-12-03  
**Version**: 1.0  
**Author**: Spring Generator Team
