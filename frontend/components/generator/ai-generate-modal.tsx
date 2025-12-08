"use client"

import { useEffect, useRef, useState, useMemo, useCallback } from "react"
import { motion } from "framer-motion"
import { X, Sparkles, Loader2, MessageSquare, Copy, RotateCcw, Download } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { useGeneratorStore, type Table, type Column, type Relationship } from "@/lib/store"
import ReactMarkdown from "react-markdown"
import debounce from "lodash/debounce"

// ---------- Types ----------
interface AiGenerateModalProps {
  onClose: () => void
  conversationMode?: boolean
}

interface AIResponse {
  sessionId: string
  actions: Action[]
  explanation?: string
}

type ActionType = "create" | "edit" | "delete" | "replace"

interface Action {
  type: ActionType
  tables?: Table[]
  tableNames?: string[]
  newSchema?: Table[]
}

// ---------- Configuration ----------
const CONVERSATION_CAP = 100
const MAX_HISTORY = 10
const MAX_TABLES = 50

// Hierarchical layout configuration
const LAYOUT_CONFIG = {
  START_X: 200,
  START_Y: 100,
  LEVEL_GAP_Y: 500,    // Vertical spacing between levels
  TABLE_GAP_X: 650,    // Horizontal spacing between tables in same level
}

// ---------- Helper utilities ----------
const now = () => Date.now()

function clampConversation(messages: any[]) {
  if (messages.length <= CONVERSATION_CAP) return messages
  return messages.slice(messages.length - CONVERSATION_CAP)
}

function prettySummary(actionsTaken: string[], detailed: string[]) {
  if (detailed.length === 0) return actionsTaken.length > 0 ? actionsTaken.join(", ") : "No changes made"
  return `AI applied:\n${detailed.map(d => `- ${d}`).join("\n")}`
}

// Build relationship graph to understand table dependencies
function buildRelationshipGraph(tables: Table[]): Map<string, Set<string>> {
  const graph = new Map<string, Set<string>>()

  tables.forEach(table => {
    const parents = new Set<string>()

    // Check foreign key columns
    table.columns?.forEach(col => {
      if (col.foreignKey && col.referencedTable) {
        parents.add(col.referencedTable.toLowerCase())
      }
    })

    // Check explicit relationships (MANY_TO_ONE means this table depends on target)
    table.relationships?.forEach(rel => {
      const relType = typeof rel.type === 'object' ? rel.type.type : rel.type
      if (relType === 'MANY_TO_ONE') {
        parents.add(rel.targetTable.toLowerCase())
      }
    })

    graph.set(table.name.toLowerCase(), parents)
  })

  return graph
}

// Assign hierarchical levels based on dependencies
function assignLevels(tables: Table[], graph: Map<string, Set<string>>): Map<string, number> {
  const levels = new Map<string, number>()
  const assigned = new Set<string>()

  let currentLevel = 0
  let remaining = new Set(tables.map(t => t.name.toLowerCase()))

  while (remaining.size > 0) {
    const canAssign: string[] = []

    remaining.forEach(tableName => {
      const parents = graph.get(tableName) || new Set()
      // Can assign if all parents are already assigned or has no parents
      const allParentsAssigned = Array.from(parents).every(p => assigned.has(p))

      if (parents.size === 0 || allParentsAssigned) {
        canAssign.push(tableName)
      }
    })

    // Assign current level
    canAssign.forEach(tableName => {
      levels.set(tableName, currentLevel)
      assigned.add(tableName)
      remaining.delete(tableName)
    })

    currentLevel++

    // Prevent infinite loop for circular dependencies
    if (canAssign.length === 0 && remaining.size > 0) {
      remaining.forEach(tableName => {
        levels.set(tableName, currentLevel)
        assigned.add(tableName)
      })
      break
    }
  }

  return levels
}

// ---------- Improved hierarchical layout with barycenter ordering ----------
/**
 * Orders nodes inside each level using barycenter heuristic (several passes)
 * to reduce edge crossings, then computes x/y positions.
 */
function calculateHierarchicalPositions(tables: Table[]): Table[] {
  if (tables.length === 0) return []

  // Build graph and level assignment (reuse your helpers)
  const graph = buildRelationshipGraph(tables)
  const levels = assignLevels(tables, graph)

  // Group tables by level (preserving insertion order)
  const tablesByLevel = new Map<number, Table[]>()
  tables.forEach(table => {
    const level = levels.get(table.name.toLowerCase()) ?? 0
    if (!tablesByLevel.has(level)) tablesByLevel.set(level, [])
    tablesByLevel.get(level)!.push(table)
  })

  // Build adjacency map (both directions) for barycenter calculation
  const adjacency = new Map<string, Set<string>>() // node -> neighbors
  tables.forEach(t => {
    adjacency.set(t.name.toLowerCase(), new Set())
  })
  tables.forEach(t => {
    const src = t.name.toLowerCase();
    (t.columns || []).forEach(col => {
      if (col.foreignKey && col.referencedTable) {
        const targ = col.referencedTable.toLowerCase()
        if (adjacency.has(src)) adjacency.get(src)!.add(targ)
        if (adjacency.has(targ)) adjacency.get(targ)!.add(src)
      }
    });
    (t.relationships || []).forEach(rel => {
      const relType = typeof rel.type === 'object' ? rel.type.type : rel.type
      const targ = rel.targetTable?.toLowerCase()
      if (targ) {
        // connect both ways (for ordering)
        adjacency.get(src)?.add(targ)
        adjacency.get(targ)?.add(src)
      }
    })
  })

  // Create mutable ordering arrays (level -> array of table names)
  const orderByLevel = new Map<number, string[]>()
  Array.from(tablesByLevel.keys()).sort((a, b) => a - b).forEach(level => {
    orderByLevel.set(level, tablesByLevel.get(level)!.map(t => t.name.toLowerCase()))
  })

  // Initialize ordering: sort by degree (more connected first) for stability
  orderByLevel.forEach((arr, level) => {
    arr.sort((a, b) => {
      const degA = adjacency.get(a)?.size ?? 0
      const degB = adjacency.get(b)?.size ?? 0
      return degB - degA
    })
  })

  // Barycenter function: compute average index of neighbors in adjacent level(s)
  function computeBarycenters(levelIndex: number, topDown: boolean) {
    const names = orderByLevel.get(levelIndex)!
    const neighborLevel = topDown ? levelIndex - 1 : levelIndex + 1
    const neighborOrder = orderByLevel.get(neighborLevel)
    const neighborIndex = new Map<string, number>()
    if (neighborOrder) neighborOrder.forEach((name, idx) => neighborIndex.set(name, idx))

    const barycenters = names.map(name => {
      // neighbors that exist in neighbor level
      let sum = 0, cnt = 0
      const neighs = adjacency.get(name) || new Set()
      neighs.forEach(n => {
        if (neighborIndex.has(n)) {
          sum += neighborIndex.get(n)!
          cnt++
        }
      })
      const bary = cnt > 0 ? sum / cnt : Number.POSITIVE_INFINITY
      return { name, bary, degree: (adjacency.get(name)?.size ?? 0) }
    })

    // Sort: finite barycenters first (ascending), then by degree desc, then keep stable
    barycenters.sort((a, b) => {
      if (a.bary === b.bary) return b.degree - a.degree
      if (!isFinite(a.bary)) return 1
      if (!isFinite(b.bary)) return -1
      return a.bary - b.bary
    })

    // Apply ordering back
    orderByLevel.set(levelIndex, barycenters.map(b => b.name))
  }

  // Do several passes top->bottom then bottom->top to reduce crossings
  const minLevel = Math.min(...Array.from(tablesByLevel.keys()))
  const maxLevel = Math.max(...Array.from(tablesByLevel.keys()))
  const passes = 3
  for (let p = 0; p < passes; p++) {
    // top-down
    for (let lvl = minLevel + 1; lvl <= maxLevel; lvl++) {
      if (!orderByLevel.has(lvl)) continue
      computeBarycenters(lvl, true)
    }
    // bottom-up
    for (let lvl = maxLevel - 1; lvl >= minLevel; lvl--) {
      if (!orderByLevel.has(lvl)) continue
      computeBarycenters(lvl, false)
    }
  }

  // Now compute positions based on ordering
  const positioned: Table[] = []
  orderByLevel.forEach((names, level) => {
    const nodes = names.map(n => tables.find(t => t.name.toLowerCase() === n)!).filter(Boolean)
    const count = nodes.length
    // compact level spacing: if only one node, center; otherwise spread centered
    const levelGapX = LAYOUT_CONFIG.TABLE_GAP_X
    const totalWidth = (count - 1) * levelGapX
    const centerX = LAYOUT_CONFIG.START_X
    const startX = centerX - totalWidth / 2

    const y = LAYOUT_CONFIG.START_Y + (level - minLevel) * LAYOUT_CONFIG.LEVEL_GAP_Y

    nodes.forEach((table, idx) => {
      const x = startX + idx * levelGapX
      positioned.push({
        ...table,
        position: { x, y }
      })
    })
  })

  return positioned
}

// Normalize backend tables to ensure they have all required frontend fields
function normalizeTable(table: any, index: number = 0, allTables: Table[] = []): Table {
  // Generate unique ID if missing
  const id = table.id || `table-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`

  // Calculate position using hierarchical layout if we have multiple tables
  // Otherwise use simple positioning
  let position = table.position
  if (!position) {
    if (allTables.length > 0) {
      // Position will be calculated by calculateHierarchicalPositions
      position = { x: 0, y: 0 }
    } else {
      // Fallback to simple positioning
      position = {
        x: LAYOUT_CONFIG.START_X + (index % 3) * LAYOUT_CONFIG.TABLE_GAP_X,
        y: LAYOUT_CONFIG.START_Y + Math.floor(index / 3) * LAYOUT_CONFIG.LEVEL_GAP_Y
      }
    }
  }

  // Ensure relationships array exists
  const relationships = table.relationships || []

  return {
    ...table,
    id,
    position,
    relationships
  }
}

// ---------- backend AI generation ----------

const aiGenerateSchema = async (
  prompt: string,
  currentTables: Table[],
  sessionId: string | null,
  allowDestructive: boolean
): Promise<AIResponse> => {

  const requestBody = {
    prompt,
    currentTables,
    sessionId,
    allowDestructive,
    timestamp: Date.now()
  }

  console.log('Sending AI request:', requestBody)

  // Call your backend API
  const response = await fetch(`${process.env.NEXT_PUBLIC_BACKEND_URL}/api/ai/generateTables`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
    body: JSON.stringify(requestBody)
  })
  if (!response.ok) {
    let errorMessage = `API Error: ${response.status}`
    try {
      const errorText = await response.text()
      if (errorText) {
        try {
          const errorJson = JSON.parse(errorText)
          errorMessage = errorJson.message || errorJson.error || errorText
        } catch {
          errorMessage = errorText
        }
      }
    } catch {
      // Ignore parsing errors
    }
    throw new Error(errorMessage)
  }
  const data = await response.json()

  return data
}

// ---------- Component ----------
export function AiGenerateModal({ onClose, conversationMode = false }: AiGenerateModalProps) {
  const [prompt, setPrompt] = useState("")
  const [isGenerating, setIsGenerating] = useState(false)
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [conversation, setConversation] = useState<Array<{
    role: 'user' | 'assistant'
    content: string
    timestamp: number
  }>>([])

  const [allowDestructive, setAllowDestructive] = useState(false)
  const [schemaHistory, setSchemaHistory] = useState<Table[][]>([])
  const [loadingSkeleton, setLoadingSkeleton] = useState(false)
  const [lastError, setLastError] = useState<string | null>(null)
  const [showExamples, setShowExamples] = useState(false)

  const { tables, setTables } = useGeneratorStore()
  const chatRef = useRef<HTMLDivElement | null>(null)
  const inputRef = useRef<HTMLTextAreaElement | null>(null)

  // Memoized values
  const tableNames = useMemo(() => tables.map(t => t.name), [tables])
  const canUndo = schemaHistory.length > 0
  const tableCountWarning = tables.length >= MAX_TABLES * 0.8

  // Auto-scroll when conversation changes
  useEffect(() => {
    if (chatRef.current) {
      chatRef.current.scrollTop = chatRef.current.scrollHeight + 100
    }
  }, [conversation, loadingSkeleton])

  // Auto-focus input
  useEffect(() => {
    if (inputRef.current && !isGenerating) {
      inputRef.current.focus()
    }
  }, [isGenerating])

  // Limit history size
  useEffect(() => {
    if (schemaHistory.length > MAX_HISTORY) {
      setSchemaHistory(prev => prev.slice(prev.length - MAX_HISTORY))
    }
  }, [schemaHistory])

  // Auto-save backup (debounced)
  const autoSave = useCallback(
    debounce((tablesToSave: Table[]) => {
      try {
        localStorage.setItem('schema-backup', JSON.stringify(tablesToSave))
      } catch (e) {
        console.warn('Failed to auto-save schema')
      }
    }, 2000),
    []
  )

  useEffect(() => {
    autoSave(tables)
  }, [tables, autoSave])

  // Add message to conversation
  const addToConversation = (role: 'user' | 'assistant', content: string) => {
    setConversation(prev => {
      const next = [...prev, { role, content, timestamp: now() }]
      return clampConversation(next)
    })
  }

  // Undo last schema change
  const undoLast = () => {
    if (!canUndo) {
      toast.error("Nothing to undo")
      return
    }

    setSchemaHistory(prev => {
      const last = prev[prev.length - 1]
      setTables(last)
      toast.success(`Reverted last change (${prev.length - 1} actions left)`)
      return prev.slice(0, prev.length - 1)
    })
  }

  // Export conversation
  const exportConversation = () => {
    const exportData = {
      sessionId,
      timestamp: new Date().toISOString(),
      conversation,
      tables: tables.map(t => ({
        name: t.name,
        columns: t.columns.map(c => c.name)
      }))
    }

    try {
      const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `ai-schema-session-${sessionId || Date.now()}.json`
      a.click()
      URL.revokeObjectURL(url)
      toast.success("Conversation exported")
    } catch (error) {
      toast.error("Failed to export conversation")
    }
  }

  // Copy session id
  const copySessionId = async () => {
    if (!sessionId) return
    try {
      await navigator.clipboard.writeText(sessionId)
      toast.success("Session ID copied")
    } catch {
      toast.error("Failed to copy")
    }
  }

  // Keyboard handling
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      if (!isGenerating) handleGenerate()
    } else if (e.key === 'Escape') {
      e.preventDefault()
      if (!isGenerating) onClose()
    } else if (e.key === 'ArrowUp' && (e.metaKey || e.ctrlKey)) {
      e.preventDefault()
      const lastUser = [...conversation].reverse().find(m => m.role === 'user')
      if (lastUser) {
        setPrompt(lastUser.content)
        inputRef.current?.focus()
      }
    }
  }

  // Main generation handler
  const handleGenerate = async () => {
    if (!prompt.trim()) {
      toast.error("Please enter a description")
      return
    }
    if (isGenerating) return

    setIsGenerating(true)
    setLoadingSkeleton(true)
    setLastError(null)
    addToConversation('user', prompt)

    try {
      // Save current schema for undo
      setSchemaHistory(prev => [...prev, JSON.parse(JSON.stringify(tables))])

      const result = await aiGenerateSchema(prompt, tables, sessionId, allowDestructive)
      console.log('Received AI response:        ', result)


      if (result.sessionId) setSessionId(result.sessionId)

      let updatedTables = [...tables]
      const actionsTaken: string[] = []
      const detailed: string[] = []

      // Process validated actions only
      for (const action of result.actions) {
        switch (action.type) {
          case "create":
            if (action.tables && Array.isArray(action.tables) && action.tables.length > 0) {
              // Check table limit
              if (updatedTables.length + action.tables.length > MAX_TABLES) {
                detailed.push(`Cannot create ${action.tables.length} tables: maximum ${MAX_TABLES} tables exceeded`)
                break
              }

              // First normalize tables to ensure they have IDs
              const normalizedTables = action.tables.map((t, i) => normalizeTable(t, i, action.tables))
              const toCreate = normalizedTables.filter(nt => !updatedTables.some(ex => ex.name === nt.name))

              // Combine with existing tables and recalculate ALL positions hierarchically
              const allTables = [...updatedTables, ...toCreate]
              updatedTables = calculateHierarchicalPositions(allTables)

              actionsTaken.push(`Created ${toCreate.length} table(s)`)
              toCreate.forEach(t => detailed.push(`Created table: ${t.name}`))
              if (toCreate.length < normalizedTables.length) {
                detailed.push(`Skipped ${(normalizedTables.length - toCreate.length)} duplicate table(s)`)
              }
            }
            break

          case "edit":
            if (action.tables && Array.isArray(action.tables) && action.tables.length > 0) {
              action.tables.forEach((newTable, idx) => {
                const index = updatedTables.findIndex(t => t.name === newTable.name)
                if (index !== -1) {
                  // Merge the edited table data
                  updatedTables[index] = {
                    ...updatedTables[index],
                    ...newTable,
                    id: updatedTables[index].id, // Preserve existing ID
                    relationships: newTable.relationships ?? updatedTables[index].relationships ?? []
                  }
                  detailed.push(`Edited table: ${newTable.name}`)
                } else {
                  // If edit targets unknown table, treat as create (safe fallback)
                  const normalized = normalizeTable(newTable, updatedTables.length, updatedTables)
                  updatedTables = [...updatedTables, normalized]
                  detailed.push(`Created fallback table: ${newTable.name}`)
                }
              })

              // Recalculate positions hierarchically after edits
              updatedTables = calculateHierarchicalPositions(updatedTables)
              actionsTaken.push(`Edited ${action.tables.length} table(s)`)
            }
            break

          case "delete":
            if (action.tableNames && Array.isArray(action.tableNames) && action.tableNames.length > 0) {
              if (!allowDestructive) {
                detailed.push(`Skipped deletion (${action.tableNames.length} tables) - destructive actions disabled`)
                break
              }
              const prevCount = updatedTables.length
              updatedTables = updatedTables.filter(t => !action.tableNames!.includes(t.name))
              const deleted = prevCount - updatedTables.length
              actionsTaken.push(`Deleted ${deleted} table(s)`)
              action.tableNames.forEach(n => detailed.push(`Deleted table: ${n}`))
            }
            break

          case "replace":
            if (!allowDestructive) {
              detailed.push(`Skipped schema replacement - destructive actions disabled`)
              break
            }
            if (action.newSchema && Array.isArray(action.newSchema)) {
              // Normalize all tables and calculate hierarchical positions
              const normalized = action.newSchema.map((t, i) => normalizeTable(t, i, action.newSchema))
              updatedTables = calculateHierarchicalPositions(normalized)
              actionsTaken.push(`Replaced entire schema with ${action.newSchema.length} table(s)`)
              detailed.push(`Replaced entire schema`)
            }
            break
        }
      }

      // Commit changes
      setTables(updatedTables)

      // Build summary
      const summary = prettySummary(actionsTaken, detailed)

      // Add assistant response
      addToConversation('assistant', result.explanation ?? summary)

      // Show toast
      toast.success(result.explanation ?? summary)

      // Reset for next interaction
      if (conversationMode) {
        setPrompt("")
      } else {
        onClose()
      }

    } catch (error: any) {
      console.error(error)
      const errorMsg = error?.message || "Failed to generate schema"
      setLastError(errorMsg)
      toast.error(errorMsg)

      // Revert last history entry if error occurred
      setSchemaHistory(prev => {
        if (prev.length === 0) return prev
        const last = prev[prev.length - 1]
        setTables(last)
        return prev.slice(0, prev.length - 1)
      })
    } finally {
      setIsGenerating(false)
      setTimeout(() => setLoadingSkeleton(false), 250)
    }
  }

  // Load from backup
  const loadFromBackup = () => {
    try {
      const backup = localStorage.getItem('schema-backup')
      if (backup) {
        const parsed = JSON.parse(backup)
        setTables(parsed)
        toast.success("Loaded from auto-save backup")
      } else {
        toast.info("No backup found")
      }
    } catch (error) {
      toast.error("Failed to load backup")
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm"
      onClick={conversationMode ? undefined : onClose}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        className="glass-strong rounded-2xl w-full max-w-2xl overflow-hidden max-h-[90vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border shrink-0">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary/20 to-accent/20 flex items-center justify-center">
              {conversationMode ? (
                <MessageSquare className="w-4 h-4 text-primary" />
              ) : (
                <Sparkles className="w-4 h-4 text-primary" />
              )}
            </div>
            <div>
              <h2 className="text-lg font-semibold">
                {conversationMode ? "AI Schema Assistant" : "AI Schema Generator"}
              </h2>
              <div className="flex items-center gap-2">
                {sessionId ? (
                  <button
                    title="Click to copy full session id"
                    onClick={copySessionId}
                    className="text-xs text-muted-foreground flex items-center gap-1 hover:text-primary transition-colors"
                    aria-label="Copy session id"
                  >
                    <span>Session: {sessionId.substring(0, 8)}...</span>
                    <Copy className="w-3 h-3 opacity-70" />
                  </button>
                ) : (
                  <p className="text-xs text-muted-foreground">Session: —</p>
                )}
                <span className="text-xs text-muted-foreground ml-2">Tables: {tables.length}</span>
                {tableCountWarning && (
                  <span className="text-xs text-amber-500">
                    ⚠️ {tables.length}/{MAX_TABLES}
                  </span>
                )}
              </div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={undoLast}
              disabled={!canUndo || isGenerating}
              className="flex items-center gap-1"
            >
              <RotateCcw className="w-3 h-3" />
              Undo ({schemaHistory.length})
            </Button>
            <button onClick={onClose} className="p-2 rounded-lg hover:bg-secondary/50 transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Conversation History */}
        {conversationMode && (
          <div className="flex-1 overflow-y-auto p-4 border-b border-border max-h-64" ref={chatRef}>
            <div className="space-y-4">
              {conversation.length === 0 && !loadingSkeleton && (
                <div className="text-sm text-muted-foreground italic text-center py-4">
                  No messages yet — describe what you want to build!
                </div>
              )}

              {conversation.map((msg, index) => (
                <div key={index} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                  <div
                    className={`max-w-[80%] rounded-lg p-3 ${msg.role === 'user'
                      ? 'bg-primary text-primary-foreground'
                      : 'bg-secondary'
                      }`}
                  >
                    {msg.role === 'assistant' ? (
                      <div className="prose prose-sm max-w-none dark:prose-invert">
                        <ReactMarkdown>{msg.content}</ReactMarkdown>
                      </div>
                    ) : (
                      <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
                    )}
                    <p className="text-xs opacity-70 mt-1">
                      {new Date(msg.timestamp).toLocaleString([], { hour: '2-digit', minute: '2-digit' })}
                    </p>
                  </div>
                </div>
              ))}

              {/* Loading skeleton */}
              {loadingSkeleton && (
                <div className="animate-pulse space-y-2">
                  <div className="w-3/4 h-4 bg-muted rounded" />
                  <div className="w-1/2 h-4 bg-muted rounded" />
                  <div className="w-2/3 h-4 bg-muted rounded" />
                </div>
              )}
            </div>
          </div>
        )}

        {/* Content */}
        <div className="p-6 shrink-0">
          {/* Example Prompts */}
          <div className="mb-4">
            <button
              onClick={() => setShowExamples(!showExamples)}
              className="text-sm text-muted-foreground hover:text-primary transition-colors flex items-center gap-1"
            >
              {showExamples ? 'Hide examples' : 'Show example prompts'} {showExamples ? '▲' : '▼'}
            </button>
            {showExamples && (
              <div className="mt-2 space-y-1">
                {EXAMPLE_PROMPTS.map((example, i) => (
                  <button
                    key={i}
                    onClick={() => {
                      setPrompt(example)
                      inputRef.current?.focus()
                    }}
                    className="text-xs text-left block w-full p-2 rounded hover:bg-secondary/50 transition-colors text-muted-foreground hover:text-foreground"
                  >
                    {example}
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="flex items-start justify-between gap-4">
            <div className="flex-1">
              <p className="text-sm text-muted-foreground mb-4">
                {conversationMode
                  ? "Continue the conversation. I'll remember what we discussed."
                  : "Describe what you want to do with your database schema. I can create new tables, edit existing ones, or delete tables."}
              </p>

              <Textarea
                ref={inputRef}
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder={
                  conversationMode
                    ? "Type your next request here..."
                    : `Examples:
• Create tables for an e-commerce platform with users, products, and orders
• Add a comments table with user_id and post_id foreign keys
• Delete the old_users table
• Modify the products table to include a discount_price column
• Replace everything with a simple blog schema`
                }
                rows={4}
                className="bg-input/50 resize-none mb-4 font-mono text-sm"
                onKeyDown={handleKeyDown}
                disabled={isGenerating}
              />

              <div className="text-xs text-muted-foreground space-y-1">
                <div><strong>Current tables:</strong> {tables.length === 0 ? "None" : tableNames.join(", ")}</div>
                {conversationMode && (
                  <div><strong>Conversation:</strong> {conversation.length} messages</div>
                )}
                {tableCountWarning && (
                  <div className="text-amber-500">
                    ⚠️ You have {tables.length} tables (approaching limit of {MAX_TABLES})
                  </div>
                )}
              </div>
            </div>

            {/* Right column: switches & controls */}
            <div className="w-48 shrink-0 space-y-4">
              <div className="text-xs">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={allowDestructive}
                    onChange={(e) => setAllowDestructive(e.target.checked)}
                    disabled={isGenerating}
                    className="cursor-pointer"
                  />
                  <span className="text-sm">Allow destructive actions</span>
                </label>
                <p className="text-muted-foreground text-xs mt-1">Enable to allow table deletion and schema replacement.</p>
              </div>

              <div className="space-y-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => { setConversation([]); toast.success("Chat cleared") }}
                  disabled={isGenerating || conversation.length === 0}
                  className="w-full"
                >
                  Clear Chat
                </Button>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={exportConversation}
                  disabled={isGenerating || conversation.length === 0}
                  className="w-full flex items-center gap-1"
                >
                  <Download className="w-3 h-3" />
                  Export Chat
                </Button>

                <Button
                  variant="ghost"
                  size="sm"
                  onClick={loadFromBackup}
                  className="w-full text-xs"
                >
                  Load Backup
                </Button>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-border shrink-0">
          <div className="text-sm text-muted-foreground">
            {conversationMode && (
              <span>Enter to send • Shift+Enter for new line • Esc to close • ↑+⌘ to recall</span>
            )}
          </div>
          <div className="flex items-center gap-3">
            {lastError && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setLastError(null)
                  handleGenerate()
                }}
                disabled={isGenerating}
              >
                Retry
              </Button>
            )}

            <Button variant="outline" onClick={onClose} disabled={isGenerating}>
              {conversationMode ? "Done" : "Cancel"}
            </Button>

            <Button
              onClick={handleGenerate}
              disabled={isGenerating || !prompt.trim() || tables.length >= MAX_TABLES}
              className="bg-gradient-to-r from-primary to-accent text-primary-foreground hover:opacity-90 transition-opacity"
            >
              {isGenerating ? (
                <span className="flex items-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Generating...
                </span>
              ) : (
                <span className="flex items-center gap-2">
                  {conversationMode ? <MessageSquare className="w-4 h-4" /> : <Sparkles className="w-4 h-4" />}
                  {conversationMode ? "Send Message" : "Generate with AI"}
                </span>
              )}
            </Button>
          </div>
        </div>
      </motion.div>
    </motion.div>
  )
}
