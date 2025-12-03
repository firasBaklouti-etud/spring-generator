import { create } from "zustand"

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

export interface RelationshipType {
  type: "ONE_TO_ONE" | "ONE_TO_MANY" | "MANY_TO_ONE" | "MANY_TO_MANY"
}

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
  groupId: string
  artifactId: string
  name: string
  description: string
  packageName: string
  javaVersion: string
  bootVersion: string
  dependencies: string[] // Store dependency IDs, convert to full objects when sending to API

  // Advanced features for code generation
  includeEntity: boolean
  includeRepository: boolean
  includeService: boolean
  includeController: boolean
  includeDto: boolean
  includeMapper: boolean
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

  // Preview files
  previewFiles: FilePreview[]
  setPreviewFiles: (files: FilePreview[]) => void
  updatePreviewFile: (path: string, content: string) => void

  // Loading states
  isParsing: boolean
  setIsParsing: (loading: boolean) => void
  isGenerating: boolean
  setIsGenerating: (loading: boolean) => void

  // Reset
  reset: () => void
}

const defaultProjectConfig: ProjectConfig = {
  groupId: "com.example",
  artifactId: "demo",
  name: "Demo",
  description: "Demo project for Spring Boot",
  packageName: "com.example.demo",
  javaVersion: "17",
  bootVersion: "3.2.0",
  dependencies: [],
  includeEntity: true,
  includeRepository: true,
  includeService: true,
  includeController: true,
  includeDto: false,
  includeMapper: false,
}

export const useGeneratorStore = create<GeneratorStore>((set, get) => ({
  currentPhase: 1,
  setCurrentPhase: (phase) => set({ currentPhase: phase }),

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

  reset: () =>
    set({
      currentPhase: 1,
      sqlInput: "",
      tables: [],
      history: [],
      historyIndex: -1,
      projectConfig: defaultProjectConfig,
      previewFiles: [],
      isParsing: false,
      isGenerating: false,
    }),
}))
