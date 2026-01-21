"use client"

import { memo } from "react"
import { Handle, Position, type NodeProps } from "@xyflow/react"
import { motion } from "framer-motion"
import { Key, Hash, Edit2, Trash2, Link } from "lucide-react"
import type { Table } from "@/lib/store"

interface TableNodeData {
  table: Table
  onEdit: () => void
  onDelete: () => void
}

export const TableNode = memo(function TableNode({ data }: NodeProps<TableNodeData>) {
  const { table, onEdit, onDelete } = data as TableNodeData

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      className="glass-strong rounded-xl overflow-hidden min-w-[260px] max-w-[320px] gradient-border"
    >
      <Handle type="target" position={Position.Left} className="!w-3 !h-3 !bg-primary !border-2 !border-background" />
      <Handle type="source" position={Position.Right} className="!w-3 !h-3 !bg-accent !border-2 !border-background" />

      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 bg-gradient-to-r from-primary/20 to-accent/20 border-b border-border">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-primary/20 flex items-center justify-center">
            <Hash className="w-4 h-4 text-primary" />
          </div>
          <div>
            <h3 className="font-semibold text-sm">{table.className}</h3>
            <p className="text-xs text-muted-foreground">{table.name}</p>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <button
            onClick={(e) => {
              e.stopPropagation()
              onEdit()
            }}
            className="p-1.5 rounded-lg hover:bg-secondary/50 transition-colors"
          >
            <Edit2 className="w-3.5 h-3.5 text-muted-foreground" />
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation()
              onDelete()
            }}
            className="p-1.5 rounded-lg hover:bg-destructive/20 transition-colors"
          >
            <Trash2 className="w-3.5 h-3.5 text-destructive" />
          </button>
        </div>
      </div>

      {/* Columns */}
      <div className="divide-y divide-border max-h-[300px] overflow-y-auto">
        {table.columns.map((column, index) => (
          <div key={index} className="flex items-center gap-2 px-4 py-2 hover:bg-secondary/30 transition-colors">
            {column.primaryKey ? (
              <Key className="w-3.5 h-3.5 text-chart-4 flex-shrink-0" />
            ) : (
              <div className="w-3.5 h-3.5" />
            )}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium truncate">{column.fieldName}</span>
                {column.unique && (
                  <span className="px-1.5 py-0.5 text-[10px] bg-chart-2/20 text-chart-2 rounded">UQ</span>
                )}
                {!column.nullable && (
                  <span className="px-1.5 py-0.5 text-[10px] bg-destructive/20 text-destructive rounded">NN</span>
                )}
              </div>
            </div>
            <span className="text-xs text-muted-foreground font-mono">{column.javaType}</span>
          </div>
        ))}
      </div>

      {/* Relationships indicator */}
      {table.relationships.length > 0 && (
        <div className="px-4 py-2 border-t border-border bg-secondary/20">
          <div className="flex items-center gap-1 text-xs text-muted-foreground">
            <Link className="w-3 h-3" />
            <span>{table.relationships.length} relationship(s)</span>
          </div>
        </div>
      )}
    </motion.div>
  )
})
