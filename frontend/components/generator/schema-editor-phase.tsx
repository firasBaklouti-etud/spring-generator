"use client"

import { useCallback, useEffect, useState } from "react"
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  addEdge,
  type Connection,
  type Edge,
  type Node,
  MarkerType,
  Panel,
} from "@xyflow/react"
import "@xyflow/react/dist/style.css"
import { motion, AnimatePresence } from "framer-motion"
import { Plus, Undo2, Redo2, Trash2, Sparkles, ArrowRight } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { useGeneratorStore, type Table, type Relationship } from "@/lib/store"
import { TableNode } from "./table-node"
import { RelationshipEdge } from "./relationship-edge"
import { AiGenerateModal } from "./ai-generate-modal"
import { TableEditor } from "./table-editor"

const nodeTypes = {
  tableNode: TableNode,
}

const edgeTypes = {
  relationshipEdge: RelationshipEdge
}

export function SchemaEditorPhase() {
  const { tables, setTables, addTable, updateTable, deleteTable, undo, redo, history, historyIndex, setCurrentPhase } =
    useGeneratorStore()

  const [nodes, setNodes, onNodesChange] = useNodesState([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const [selectedTable, setSelectedTable] = useState<Table | null>(null)
  const [showAiModal, setShowAiModal] = useState(false)

  // Convert tables to React Flow nodes
  useEffect(() => {
    const flowNodes: Node[] = tables.map((table) => ({
      id: table.id,
      type: "tableNode",
      position: table.position || { x: 0, y: 0 },
      data: {
        table,
        onEdit: () => setSelectedTable(table),
        onDelete: () => handleDeleteTable(table.id),
      },
    }))
    setNodes(flowNodes)

    const flowEdges: Edge[] = []
    const addedEdges = new Set<string>()

    // First, create edges from explicit relationships
    tables.forEach((table) => {
      table.relationships.forEach((rel) => {
        const targetTable = tables.find((t) => t.name.toLowerCase() === rel.targetTable.toLowerCase())
        if (targetTable) {
          const edgeId = `${table.id}-${targetTable.id}-${rel.fieldName}`
          const reverseEdgeId = `${targetTable.id}-${table.id}-${rel.fieldName}`

          // Skip if we've already added this edge or its reverse
          if (addedEdges.has(edgeId) || addedEdges.has(reverseEdgeId)) {
            return
          }

          const relType = typeof rel.type === "object" ? rel.type.type : rel.type

          // Determine source and target based on relationship type
          let sourceId = table.id
          let targetId = targetTable.id

          let sourceLabel = "1"
          let targetLabel = "N"

          // For MANY_TO_ONE, reverse the direction for visual clarity
          if (relType === "MANY_TO_ONE") {
            sourceId = targetTable.id
            targetId = table.id
            sourceLabel = "1"
            targetLabel = "N"
          } else if (relType === "ONE_TO_ONE") {
            sourceLabel = "1"
            targetLabel = "1"
          } else if (relType === "MANY_TO_MANY") {
            sourceLabel = "N"
            targetLabel = "N"
          }

          const finalEdgeId = `${sourceId}-${targetId}-${rel.fieldName}`

          flowEdges.push({
            id: finalEdgeId,
            source: sourceId,
            target: targetId,
            type: "relationshipEdge",
            data: { sourceLabel, targetLabel },
            animated: true,
            style: {
              stroke: "oklch(0.7 0.18 200)",
              strokeWidth: 2,
              strokeDasharray: relType === "MANY_TO_MANY" ? "5,5" : undefined,
            },
            markerEnd: {
              type: MarkerType.ArrowClosed,
              color: "oklch(0.7 0.18 200)",
            },
          })
          addedEdges.add(finalEdgeId)
        }
      })
    })

    // Then, add edges from foreign key columns ONLY if no relationship already exists
    tables.forEach((table) => {
      table.columns.forEach((column) => {
        if (column.foreignKey && column.referencedTable) {
          const referencedTable = tables.find((t) => t.name.toLowerCase() === column.referencedTable?.toLowerCase())
          if (referencedTable) {
            // Check if a relationship already exists between these tables
            const existingRelationship = table.relationships.some(
              (rel) => rel.targetTable.toLowerCase() === referencedTable.name.toLowerCase()
            )

            // Check if we already have an edge between these tables (in either direction)
            const existingEdge = Array.from(addedEdges).some(
              (id) => id.includes(`${table.id}-${referencedTable.id}`) || id.includes(`${referencedTable.id}-${table.id}`)
            )

            if (!existingRelationship && !existingEdge) {
              const edgeId = `fk-${referencedTable.id}-${table.id}-${column.name}`

              flowEdges.push({
                id: edgeId,
                source: referencedTable.id,
                target: table.id,
                type: "relationshipEdge",
                data: { sourceLabel: "1", targetLabel: "N" },
                animated: true,
                style: { stroke: "oklch(0.65 0.15 50)", strokeWidth: 2 },
                markerEnd: {
                  type: MarkerType.ArrowClosed,
                  color: "oklch(0.65 0.15 50)",
                }
              })
              addedEdges.add(edgeId)
            }
          }
        }
      })
    })
    setEdges(flowEdges)
  }, [tables, setNodes, setEdges])

  // Update table positions when nodes are dragged
  const handleNodesChange = useCallback(
    (changes: Parameters<typeof onNodesChange>[0]) => {
      onNodesChange(changes)

      changes.forEach((change) => {
        if (change.type === "position" && change.position) {
          const table = tables.find((t) => t.id === change.id)
          if (table && table.position) {
            const newPos = change.position
            if (Math.abs(table.position.x - newPos.x) > 1 || Math.abs(table.position.y - newPos.y) > 1) {
              if (!change.dragging) {
                updateTable(change.id, { position: newPos })
              }
            }
          }
        }
      })
    },
    [onNodesChange, tables, updateTable],
  )

  // Handle new edge connections
  const onConnect = useCallback(
    (params: Connection) => {
      const sourceTable = tables.find((t) => t.id === params.source)
      const targetTable = tables.find((t) => t.id === params.target)

      if (sourceTable && targetTable) {
        const newRelationship: Relationship = {
          type: { type: "ONE_TO_MANY" },
          sourceTable: sourceTable.name,
          targetTable: targetTable.name,
          sourceColumn: "id",
          targetColumn: targetTable.name.toLowerCase() + "_id",
          fieldName: targetTable.name.toLowerCase() + "s",
          targetClassName: targetTable.className,
        }

        updateTable(sourceTable.id, {
          relationships: [...sourceTable.relationships, newRelationship],
        })

        toast.success(`Created relationship: ${sourceTable.name} → ${targetTable.name}`)
      }

      setEdges((eds) =>
        addEdge(
          {
            ...params,
            type: "relationshipEdge",
            data: { sourceLabel: "1", targetLabel: "N" },
            animated: true,
            style: { stroke: "oklch(0.7 0.18 200)", strokeWidth: 2 },
            markerEnd: {
              type: MarkerType.ArrowClosed,
              color: "oklch(0.7 0.18 200)",
            },
          },
          eds,
        ),
      )
    },
    [tables, updateTable, setEdges],
  )

  const handleDeleteTable = (id: string) => {
    deleteTable(id)
    toast.success("Table deleted")
  }

  const handleAddTable = () => {
    const newTable: Table = {
      id: `table-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      name: `new_table_${tables.length + 1}`,
      className: `NewTable${tables.length + 1}`,
      columns: [
        {
          name: "id",
          type: "BIGINT",
          fieldName: "id",
          javaType: "Long",
          primaryKey: true,
          autoIncrement: true,
          nullable: false,
          foreignKey: false,
          unique: false,
        },
      ],
      relationships: [],
      isJoinTable: false,
      position: { x: 100 + Math.random() * 200, y: 100 + Math.random() * 200 },
    }
    addTable(newTable)
    toast.success("New table added")
  }

  const handleClearAll = () => {
    if (confirm("Are you sure you want to clear all tables?")) {
      setTables([])
      toast.success("All tables cleared")
    }
  }

  const handleSaveTable = (updatedTable: Table) => {
    updateTable(updatedTable.id, updatedTable)
    setSelectedTable(null)
    toast.success("Table updated")
  }

  const canUndo = historyIndex > 0
  const canRedo = historyIndex < history.length - 1

  return (
    <div className="h-[calc(100vh-8rem)] flex flex-col">
      {/* Toolbar */}
      <div className="border-b border-border bg-card/30 backdrop-blur-sm px-4 py-2">
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={handleAddTable} className="glass bg-transparent">
              <Plus className="w-4 h-4 mr-1" />
              Add Table
            </Button>
            <Button variant="outline" size="sm" onClick={() => setShowAiModal(true)} className="glass">
              <Sparkles className="w-4 h-4 mr-1" />
              AI Generate
            </Button>
            <div className="w-px h-6 bg-border mx-2" />
            <Button variant="ghost" size="sm" onClick={undo} disabled={!canUndo} className="text-muted-foreground">
              <Undo2 className="w-4 h-4" />
            </Button>
            <Button variant="ghost" size="sm" onClick={redo} disabled={!canRedo} className="text-muted-foreground">
              <Redo2 className="w-4 h-4" />
            </Button>
            <div className="w-px h-6 bg-border mx-2" />
            <Button
              variant="ghost"
              size="sm"
              onClick={handleClearAll}
              className="text-destructive hover:text-destructive"
            >
              <Trash2 className="w-4 h-4 mr-1" />
              Clear All
            </Button>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">{tables.length} table(s)</span>
            <Button
              onClick={() => setCurrentPhase(3)}
              className="bg-gradient-to-r from-primary to-accent text-primary-foreground hover:opacity-90"
            >
              Configure Security
              <ArrowRight className="w-4 h-4 ml-1" />
            </Button>
          </div>
        </div>
      </div>

      {/* Canvas */}
      <div className="flex-1 relative">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={handleNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          fitView
          snapToGrid
          snapGrid={[20, 20]}
          minZoom={0.1}
          maxZoom={2}
          defaultEdgeOptions={{
            type: "smoothstep",
            animated: true,
          }}
          proOptions={{ hideAttribution: true }}
          className="bg-background"
        >
          <Background color="oklch(0.25 0.02 260)" gap={20} size={1} />
          <Controls
            className="glass rounded-xl overflow-hidden"
            style={{
              button: {
                backgroundColor: "oklch(0.12 0.01 260)",
                borderColor: "oklch(0.25 0.02 260)",
                color: "oklch(0.95 0 0)",
              },
            }}
          />
          <MiniMap
            nodeColor="oklch(0.7 0.18 200)"
            maskColor="oklch(0.08 0.01 260 / 0.8)"
            className="glass rounded-xl overflow-hidden"
            style={{
              backgroundColor: "oklch(0.12 0.01 260)",
            }}
          />
          <Panel position="bottom-center" className="mb-4">
            <div className="glass rounded-xl px-4 py-2 text-sm text-muted-foreground">
              Drag to connect tables • Click to edit • Scroll to zoom
            </div>
          </Panel>
        </ReactFlow>

        {/* Empty State */}
        {tables.length === 0 && (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <motion.div initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} className="text-center">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary/20 to-accent/20 flex items-center justify-center mx-auto mb-4">
                <Plus className="w-8 h-8 text-primary" />
              </div>
              <h3 className="text-lg font-semibold mb-2">No tables yet</h3>
              <p className="text-muted-foreground text-sm mb-4">Add a table or use AI to generate a schema</p>
              <div className="flex gap-2 justify-center pointer-events-auto">
                <Button onClick={handleAddTable} variant="outline" className="glass bg-transparent">
                  <Plus className="w-4 h-4 mr-1" />
                  Add Table
                </Button>
                <Button
                  onClick={() => setShowAiModal(true)}
                  className="bg-gradient-to-r from-primary to-accent text-primary-foreground"
                >
                  <Sparkles className="w-4 h-4 mr-1" />
                  AI Generate
                </Button>
              </div>
            </motion.div>
          </div>
        )}
      </div>

      {/* Table Editor Modal */}
      <AnimatePresence>
        {selectedTable && (
          <TableEditor table={selectedTable} onSave={handleSaveTable} onClose={() => setSelectedTable(null)} />
        )}
      </AnimatePresence>

      {/* AI Generate Modal */}
      <AnimatePresence>{showAiModal && <AiGenerateModal onClose={() => setShowAiModal(false)} />}</AnimatePresence>
    </div>
  )
}
