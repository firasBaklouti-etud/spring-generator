"use client"

import { useState } from "react"
import { motion } from "framer-motion"
import { X, Plus, Trash2, GripVertical, Key, ChevronDown } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import type { Table, Column } from "@/lib/store"

interface TableEditorProps {
  table: Table
  onSave: (table: Table) => void
  onClose: () => void
}

const javaTypes = [
  "Long",
  "Integer",
  "String",
  "Boolean",
  "LocalDateTime",
  "LocalDate",
  "BigDecimal",
  "Float",
  "Double",
]

const sqlTypes = ["BIGINT", "INT", "VARCHAR", "TEXT", "BOOLEAN", "TIMESTAMP", "DATE", "DECIMAL", "FLOAT", "DOUBLE"]

function toCamelCase(str: string): string {
  return str.toLowerCase().replace(/_([a-z])/g, (_, letter) => letter.toUpperCase())
}

function toPascalCase(str: string): string {
  const camel = toCamelCase(str)
  return camel.charAt(0).toUpperCase() + camel.slice(1)
}

export function TableEditor({ table, onSave, onClose }: TableEditorProps) {
  const [editedTable, setEditedTable] = useState<Table>({ ...table })
  const [expandedColumn, setExpandedColumn] = useState<number | null>(null)

  const handleTableNameChange = (name: string) => {
    setEditedTable({
      ...editedTable,
      name,
      className: toPascalCase(name),
    })
  }

  const handleColumnChange = (index: number, updates: Partial<Column>) => {
    const newColumns = [...editedTable.columns]
    newColumns[index] = { ...newColumns[index], ...updates }

    // Auto-update fieldName if name changes
    if (updates.name) {
      newColumns[index].fieldName = toCamelCase(updates.name)
    }

    setEditedTable({ ...editedTable, columns: newColumns })
  }

  const handleAddColumn = () => {
    const newColumn: Column = {
      name: `column_${editedTable.columns.length + 1}`,
      fieldName: `column${editedTable.columns.length + 1}`,
      javaType: "String",
      sqlType: "VARCHAR",
      length: 255,
      primaryKey: false,
      autoIncrement: false,
      nullable: true,
      unique: false,
    }
    setEditedTable({
      ...editedTable,
      columns: [...editedTable.columns, newColumn],
    })
    setExpandedColumn(editedTable.columns.length)
  }

  const handleDeleteColumn = (index: number) => {
    const newColumns = editedTable.columns.filter((_, i) => i !== index)
    setEditedTable({ ...editedTable, columns: newColumns })
    setExpandedColumn(null)
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm"
      onClick={onClose}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        className="glass-strong rounded-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border">
          <h2 className="text-lg font-semibold">Edit Table</h2>
          <button onClick={onClose} className="p-2 rounded-lg hover:bg-secondary/50 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto max-h-[calc(90vh-8rem)]">
          {/* Table Name */}
          <div className="mb-6">
            <Label htmlFor="tableName" className="text-sm font-medium mb-2 block">
              Table Name
            </Label>
            <Input
              id="tableName"
              value={editedTable.name}
              onChange={(e) => handleTableNameChange(e.target.value)}
              className="bg-input/50"
            />
            <p className="text-xs text-muted-foreground mt-1">Class Name: {editedTable.className}</p>
          </div>

          {/* Columns */}
          <div className="mb-6">
            <div className="flex items-center justify-between mb-3">
              <Label className="text-sm font-medium">Columns</Label>
              <Button variant="outline" size="sm" onClick={handleAddColumn}>
                <Plus className="w-4 h-4 mr-1" />
                Add Column
              </Button>
            </div>

            <div className="space-y-2">
              {editedTable.columns.map((column, index) => (
                <div key={index} className="glass rounded-xl overflow-hidden">
                  {/* Column Header */}
                  <button
                    onClick={() => setExpandedColumn(expandedColumn === index ? null : index)}
                    className="w-full flex items-center gap-3 px-4 py-3 hover:bg-secondary/30 transition-colors"
                  >
                    <GripVertical className="w-4 h-4 text-muted-foreground" />
                    {column.primaryKey && <Key className="w-4 h-4 text-chart-4" />}
                    <span className="font-medium flex-1 text-left">{column.fieldName}</span>
                    <span className="text-sm text-muted-foreground font-mono">{column.javaType}</span>
                    <ChevronDown
                      className={`w-4 h-4 text-muted-foreground transition-transform ${
                        expandedColumn === index ? "rotate-180" : ""
                      }`}
                    />
                  </button>

                  {/* Expanded Column Details */}
                  {expandedColumn === index && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: "auto", opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      className="px-4 pb-4 border-t border-border"
                    >
                      <div className="grid grid-cols-2 gap-4 pt-4">
                        <div>
                          <Label className="text-xs mb-1 block">Column Name</Label>
                          <Input
                            value={column.name}
                            onChange={(e) => handleColumnChange(index, { name: e.target.value })}
                            className="bg-input/50 h-9"
                          />
                        </div>
                        <div>
                          <Label className="text-xs mb-1 block">Field Name</Label>
                          <Input
                            value={column.fieldName}
                            onChange={(e) =>
                              handleColumnChange(index, {
                                fieldName: e.target.value,
                              })
                            }
                            className="bg-input/50 h-9"
                          />
                        </div>
                        <div>
                          <Label className="text-xs mb-1 block">Java Type</Label>
                          <Select
                            value={column.javaType}
                            onValueChange={(value) => handleColumnChange(index, { javaType: value })}
                          >
                            <SelectTrigger className="bg-input/50 h-9">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {javaTypes.map((type) => (
                                <SelectItem key={type} value={type}>
                                  {type}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>
                        <div>
                          <Label className="text-xs mb-1 block">SQL Type</Label>
                          <Select
                            value={column.sqlType}
                            onValueChange={(value) => handleColumnChange(index, { sqlType: value })}
                          >
                            <SelectTrigger className="bg-input/50 h-9">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {sqlTypes.map((type) => (
                                <SelectItem key={type} value={type}>
                                  {type}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>
                        {(column.sqlType === "VARCHAR" || column.sqlType === "DECIMAL") && (
                          <div>
                            <Label className="text-xs mb-1 block">Length</Label>
                            <Input
                              type="number"
                              value={column.length || ""}
                              onChange={(e) =>
                                handleColumnChange(index, {
                                  length: Number.parseInt(e.target.value) || undefined,
                                })
                              }
                              className="bg-input/50 h-9"
                            />
                          </div>
                        )}
                      </div>

                      <div className="flex flex-wrap gap-4 mt-4 pt-4 border-t border-border">
                        <div className="flex items-center gap-2">
                          <Switch
                            checked={column.primaryKey}
                            onCheckedChange={(checked) => handleColumnChange(index, { primaryKey: checked })}
                          />
                          <Label className="text-xs">Primary Key</Label>
                        </div>
                        <div className="flex items-center gap-2">
                          <Switch
                            checked={column.autoIncrement}
                            onCheckedChange={(checked) =>
                              handleColumnChange(index, {
                                autoIncrement: checked,
                              })
                            }
                          />
                          <Label className="text-xs">Auto Increment</Label>
                        </div>
                        <div className="flex items-center gap-2">
                          <Switch
                            checked={!column.nullable}
                            onCheckedChange={(checked) => handleColumnChange(index, { nullable: !checked })}
                          />
                          <Label className="text-xs">Not Null</Label>
                        </div>
                        <div className="flex items-center gap-2">
                          <Switch
                            checked={column.unique}
                            onCheckedChange={(checked) => handleColumnChange(index, { unique: checked })}
                          />
                          <Label className="text-xs">Unique</Label>
                        </div>
                      </div>

                      <div className="flex justify-end mt-4">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDeleteColumn(index)}
                          className="text-destructive hover:text-destructive"
                        >
                          <Trash2 className="w-4 h-4 mr-1" />
                          Delete Column
                        </Button>
                      </div>
                    </motion.div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-border">
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button
            onClick={() => onSave(editedTable)}
            className="bg-gradient-to-r from-primary to-accent text-primary-foreground"
          >
            Save Changes
          </Button>
        </div>
      </motion.div>
    </motion.div>
  )
}
