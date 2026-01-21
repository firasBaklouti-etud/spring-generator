import { create } from "zustand"

export type StackType = "SPRING" | "NODE" | "NEST" | "FASTAPI"

export interface StackInfo {
  id: StackType
  displayName: string
  language: string
  defaultVersion: string
  icon: string
}

export const AVAILABLE_STACKS: StackInfo[] = [
  { id: "SPRING", displayName: "Spring Boot", language: "java", defaultVersion: "17", icon: "🍃" },
  { id: "NODE", displayName: "Node.js", language: "javascript", defaultVersion: "20", icon: "🟢" },
  { id: "NEST", displayName: "NestJS", language: "typescript", defaultVersion: "20", icon: "🐱" },
  { id: "FASTAPI", displayName: "FastAPI", language: "python", defaultVersion: "3.11", icon: "⚡" },
]

export interface SpringConfig {
  groupId: string
  artifactId: string
  javaVersion: string
  bootVersion: string
  buildTool: "maven" | "gradle"
  packaging: "jar" | "war"
}

export interface NodeConfig {
  nodeVersion: string
  packageManager: "npm" | "yarn" | "pnpm"
  useTypeScript: boolean
  orm: "prisma" | "sequelize" | "typeorm"
}

export interface NestConfig {
  nodeVersion: string
  packageManager: "npm" | "yarn" | "pnpm"
  orm: "typeorm" | "prisma" | "mikro-orm"
  useSwagger: boolean
  useValidation: boolean
}

export interface FastAPIConfig {
  pythonVersion: string
  packageManager: "pip" | "poetry" | "pipenv"
  orm: "sqlalchemy" | "tortoise" | "sqlmodel"
  useAsync: boolean
  useAlembic: boolean
}

// Type definitions for the data contracts
export interface Column {
  name: string
  type: string // SQL type
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

export type RelationshipType = "ONE_TO_ONE" | "ONE_TO_MANY" | "MANY_TO_ONE" | "MANY_TO_MANY"

export interface Relationship {
  type: RelationshipType
  sourceTable: string
  targetTable: string
  sourceColumn?: string
  targetColumn?: string
  joinTable?: string
  mappedBy?: string
  fieldName: string
  targetClassName?: string
}

export interface Table {
  id: string
  name: string
  className: string
  columns: Column[]
  relationships: Relationship[]
  isJoinTable: boolean
  position?: { x: number; y: number }
}

export interface Dependency {
  id: string
  name: string
  description: string
  groupId: string
  artifactId: string
  version?: string
  scope: string
  isStarter: boolean
}

export interface DependencyGroup {
  name: string
  dependencies: Dependency[]
}

export interface ProjectConfig {
  // Stack selection
  stackType: StackType

  // Common fields (all stacks)
  name: string
  description: string
  packageName: string
  dependencies: string[] // Store dependency IDs

  // Advanced features for code generation
  includeEntity: boolean
  includeRepository: boolean
  includeService: boolean
  includeController: boolean
  includeDto: boolean
  includeMapper: boolean
  includeTests: boolean
  includeDocker: boolean

  // Stack-specific configs
  springConfig: SpringConfig
  nodeConfig: NodeConfig
  nestConfig: NestConfig
  fastapiConfig: FastAPIConfig
  securityConfig: SecurityConfig
}

export interface SecurityRule {
  path: string
  method: string
  rule: string // PERMIT_ALL, AUTHENTICATED, HAS_ROLE
  role?: string
}

export interface SecurityConfig {
  enabled: boolean
  authenticationType?: "BASIC" | "JWT" | "OAUTH2"
  useDbAuth: boolean
  rules?: SecurityRule[]
  // Advanced Security
  principalEntity?: string // Name of the entity (e.g. "User")
  usernameField?: string   // Name of the field (e.g. "email")
  passwordField?: string   // Name of the field (e.g. "password")
  roleStrategy?: "STRING" | "ENTITY" // Legacy (keep for now or remove if breaking)
  rbacMode?: "STATIC" | "DYNAMIC"    // NEW: Dual-Mode RBAC
  roleEntity?: string      // Name of the role entity (for Dynamic mode)

  // RBAC Configuration
  permissions?: string[]   // List of available permissions (e.g. "user:read")
  definedRoles?: { name: string, permissions: string[] }[] // Role definitions
}


export interface FilePreview {
  path: string
  content: string
  language: string
}

interface GeneratorStore {
  // Current phase
  currentPhase: number
  setCurrentPhase: (phase: number) => void

  // SQL Dialect
  sqlDialect: string
  setSqlDialect: (dialect: string) => void

  // SQL input
  sqlInput: string
  setSqlInput: (sql: string) => void

  // Parsed tables
  tables: Table[]
  setTables: (tables: Table[]) => void
  addTable: (table: Table) => void
  updateTable: (id: string, updates: Partial<Table>) => void
  deleteTable: (id: string) => void

  // History for undo/redo
  history: Table[][]
  historyIndex: number
  pushHistory: () => void
  undo: () => void
  redo: () => void

  // Project configuration
  projectConfig: ProjectConfig
  setProjectConfig: (config: Partial<ProjectConfig>) => void
  setSpringConfig: (config: Partial<SpringConfig>) => void
  setNodeConfig: (config: Partial<NodeConfig>) => void
  setNestConfig: (config: Partial<NestConfig>) => void
  setFastAPIConfig: (config: Partial<FastAPIConfig>) => void

  // Preview files
  previewFiles: FilePreview[]
  setPreviewFiles: (files: FilePreview[]) => void
  updatePreviewFile: (path: string, content: string) => void

  // Loading states
  isParsing: boolean
  setIsParsing: (loading: boolean) => void
  isGenerating: boolean
  setIsGenerating: (loading: boolean) => void

  // Dependencies
  dependencyGroups: DependencyGroup[]
  setDependencyGroups: (groups: DependencyGroup[]) => void

  setSecurityConfig: (config: Partial<SecurityConfig>) => void
  addSecurityRule: (rule: SecurityRule) => void
  removeSecurityRule: (path: string, method: string) => void
  updateSecurityRule: (path: string, method: string, updates: Partial<SecurityRule>) => void

  // Reset
  reset: () => void

}

const defaultSpringConfig: SpringConfig = {
  groupId: "com.example",
  artifactId: "demo",
  javaVersion: "17",
  bootVersion: "3.2.0",
  buildTool: "maven",
  packaging: "jar",
}

const defaultNodeConfig: NodeConfig = {
  nodeVersion: "20",
  packageManager: "npm",
  useTypeScript: true,
  orm: "prisma",
}

const defaultNestConfig: NestConfig = {
  nodeVersion: "20",
  packageManager: "npm",
  orm: "typeorm",
  useSwagger: true,
  useValidation: true,
}

const defaultFastAPIConfig: FastAPIConfig = {
  pythonVersion: "3.11",
  packageManager: "poetry",
  orm: "sqlalchemy",
  useAsync: true,
  useAlembic: true,
}

const defaultProjectConfig: ProjectConfig = {
  stackType: "SPRING",
  name: "Demo",
  description: "Demo project",
  packageName: "com.example.demo",
  dependencies: [],
  includeEntity: true,
  includeRepository: true,
  includeService: true,
  includeController: true,
  includeDto: false,
  includeMapper: false,
  includeTests: false,
  includeDocker: false,
  springConfig: defaultSpringConfig,
  nodeConfig: defaultNodeConfig,
  nestConfig: defaultNestConfig,
  fastapiConfig: defaultFastAPIConfig,
  securityConfig: {
    enabled: false,
    authenticationType: "BASIC",
    useDbAuth: false,
  },
}

export const useGeneratorStore = create<GeneratorStore>((set, get) => ({
  currentPhase: 1,
  setCurrentPhase: (phase) => set({ currentPhase: phase }),

  sqlDialect: "mysql",
  setSqlDialect: (dialect) => set({ sqlDialect: dialect }),

  sqlInput: "",
  setSqlInput: (sql) => set({ sqlInput: sql }),

  tables: [],
  setTables: (tables) => {
    get().pushHistory()
    set({ tables })
  },
  addTable: (table) => {
    get().pushHistory()
    set((state) => ({ tables: [...state.tables, table] }))
  },
  updateTable: (id, updates) => {
    get().pushHistory()
    set((state) => ({
      tables: state.tables.map((t) => (t.id === id ? { ...t, ...updates } : t)),
    }))
  },
  deleteTable: (id) => {
    get().pushHistory()
    set((state) => ({
      tables: state.tables.filter((t) => t.id !== id),
    }))
  },

  history: [],
  historyIndex: -1,
  pushHistory: () => {
    const { tables, history, historyIndex } = get()
    const newHistory = history.slice(0, historyIndex + 1)
    newHistory.push(JSON.parse(JSON.stringify(tables)))
    set({ history: newHistory, historyIndex: newHistory.length - 1 })
  },
  undo: () => {
    const { history, historyIndex } = get()
    if (historyIndex > 0) {
      set({
        tables: JSON.parse(JSON.stringify(history[historyIndex - 1])),
        historyIndex: historyIndex - 1,
      })
    }
  },
  redo: () => {
    const { history, historyIndex } = get()
    if (historyIndex < history.length - 1) {
      set({
        tables: JSON.parse(JSON.stringify(history[historyIndex + 1])),
        historyIndex: historyIndex + 1,
      })
    }
  },

  projectConfig: defaultProjectConfig,
  setProjectConfig: (config) =>
    set((state) => ({
      projectConfig: { ...state.projectConfig, ...config },
    })),
  setSpringConfig: (config) =>
    set((state) => ({
      projectConfig: {
        ...state.projectConfig,
        springConfig: { ...state.projectConfig.springConfig, ...config },
      },
    })),
  setNodeConfig: (config) =>
    set((state) => ({
      projectConfig: {
        ...state.projectConfig,
        nodeConfig: { ...state.projectConfig.nodeConfig, ...config },
      },
    })),
  setNestConfig: (config) =>
    set((state) => ({
      projectConfig: {
        ...state.projectConfig,
        nestConfig: { ...state.projectConfig.nestConfig, ...config },
      },
    })),
  setFastAPIConfig: (config) =>
    set((state) => ({
      projectConfig: {
        ...state.projectConfig,
        fastapiConfig: { ...state.projectConfig.fastapiConfig, ...config },
      },
    })),
  setSecurityConfig: (config: Partial<SecurityConfig>) =>
    set((state) => ({
      projectConfig: {
        ...state.projectConfig,
        securityConfig: { ...state.projectConfig.securityConfig, ...config }
      }
    })),
  addSecurityRule: (rule) =>
    set((state) => {
      const currentRules = state.projectConfig.securityConfig.rules || []
      return {
        projectConfig: {
          ...state.projectConfig,
          securityConfig: {
            ...state.projectConfig.securityConfig,
            rules: [...currentRules, rule]
          }
        }
      }
    }),
  removeSecurityRule: (path, method) =>
    set((state) => {
      const currentRules = state.projectConfig.securityConfig.rules || []
      return {
        projectConfig: {
          ...state.projectConfig,
          securityConfig: {
            ...state.projectConfig.securityConfig,
            rules: currentRules.filter(r => !(r.path === path && r.method === method))
          }
        }
      }
    }),
  updateSecurityRule: (path, method, updates) =>
    set((state) => {
      const currentRules = state.projectConfig.securityConfig.rules || []
      return {
        projectConfig: {
          ...state.projectConfig,
          securityConfig: {
            ...state.projectConfig.securityConfig,
            rules: currentRules.map(r => (r.path === path && r.method === method) ? { ...r, ...updates } : r)
          }
        }
      }
    }),

  previewFiles: [],
  setPreviewFiles: (files) => set({ previewFiles: files }),
  updatePreviewFile: (path, content) =>
    set((state) => ({
      previewFiles: state.previewFiles.map((f) => (f.path === path ? { ...f, content } : f)),
    })),

  isParsing: false,
  setIsParsing: (loading) => set({ isParsing: loading }),
  isGenerating: false,
  setIsGenerating: (loading) => set({ isGenerating: loading }),

  dependencyGroups: [],
  setDependencyGroups: (groups) => set({ dependencyGroups: groups }),

  reset: () =>
    set({
      currentPhase: 1,
      sqlDialect: "mysql",
      sqlInput: "",
      tables: [],
      history: [],
      historyIndex: -1,
      projectConfig: defaultProjectConfig,
      previewFiles: [],
      isParsing: false,
      isGenerating: false,
      dependencyGroups: [],
    }),
}))
