"use client"

import { useState } from "react"
import { motion } from "framer-motion"
import { X, Sparkles, Loader2 } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { useGeneratorStore, type Table } from "@/lib/store"

interface AiGenerateModalProps {
  onClose: () => void
}

// AI-powered schema generation using Claude
const aiGenerateSchema = async (prompt: string, currentTables: Table[]): Promise<{ action: string; tables: Table[] }> => {
  try {
    // Prepare current schema context for Claude
    const currentSchemaContext = currentTables.length > 0
      ? `Current database schema:\n${JSON.stringify(currentTables.map(t => ({
        name: t.name,
        className: t.className,
        columns: t.columns.map(c => ({
          name: c.name,
          type: c.javaType,
          primaryKey: c.primaryKey,
          nullable: c.nullable
        })),
        relationships: t.relationships
      })), null, 2)}`
      : "No tables currently exist in the schema."

    const systemPrompt = `You are a database schema designer. The user will describe what they want to do with their database schema.

${currentSchemaContext}

Based on the user's request, you should:
1. Analyze if they want to CREATE new tables, DELETE existing tables, or EDIT/MODIFY existing tables
2. Generate appropriate table structures with proper columns, types, and relationships
3. Use proper naming conventions (snake_case for table/column names, PascalCase for class names)

Respond ONLY with valid JSON in this exact format:
{
  "action": "create" | "delete" | "edit" | "replace",
  "tables": [
    {
      "id": "table-timestamp-name",
      "name": "table_name",
      "className": "ClassName",
      "columns": [
        {
          "name": "column_name",
          "fieldName": "fieldName",
          "javaType": "String|Long|Integer|Boolean|BigDecimal|LocalDateTime|LocalDate",
          "sqlType": "VARCHAR|BIGINT|INT|BOOLEAN|DECIMAL|TIMESTAMP|DATE|TEXT",
          "length": 255,
          "primaryKey": true|false,
          "autoIncrement": true|false,
          "nullable": true|false,
          "unique": true|false
        }
      ],
      "relationships": [],
      "position": { "x": 100, "y": 100 }
    }
  ],
  "deleteTables": ["table_name1", "table_name2"],
  "explanation": "Brief explanation of what was done"
}

Important:
- For "create": Generate new tables to add to existing schema
- For "delete": Specify table names to remove in deleteTables array
- For "edit": Generate modified versions of existing tables
- For "replace": Replace entire schema with new tables
- Always include proper primary keys, timestamps, and foreign key columns where relationships exist
- Use appropriate SQL types and Java types
- Set proper nullable, unique, and autoIncrement flags`

    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "claude-sonnet-4-20250514",
        max_tokens: 4000,
        system: systemPrompt,
        messages: [
          {
            role: "user",
            content: prompt
          }
        ],
      })
    })

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`)
    }

    const data = await response.json()

    // Extract text from response
    const fullResponse = data.content
      .map(item => (item.type === "text" ? item.text : ""))
      .filter(Boolean)
      .join("\n")

    // Clean and parse JSON response
    const cleanedResponse = fullResponse
      .replace(/```json\n?/g, "")
      .replace(/```\n?/g, "")
      .trim()

    const result = JSON.parse(cleanedResponse)

    return result
  } catch (error) {
    console.error("AI Generation Error:", error)
    throw new Error("Failed to generate schema. Please try again.")
  }
}

export function AiGenerateModal({ onClose }: AiGenerateModalProps) {
  const [prompt, setPrompt] = useState("")
  const [isGenerating, setIsGenerating] = useState(false)
  const { tables, setTables } = useGeneratorStore()

  const handleGenerate = async () => {
    if (!prompt.trim()) {
      toast.error("Please enter a description")
      return
    }

    setIsGenerating(true)

    try {
      const result = await aiGenerateSchema(prompt, tables)

      let updatedTables = [...tables]
      let message = ""

      switch (result.action) {
        case "create":
          // Add new tables to existing schema
          updatedTables = [
            ...tables,
            ...result.tables.map((t, i) => ({
              ...t,
              position: {
                x: (t.position?.x || 100) + (tables.length + i) * 350,
                y: t.position?.y || 100
              }
            }))
          ]
          message = `Created ${result.tables.length} new table(s)`
          break

        case "delete":
          // Remove specified tables
          if (result.deleteTables && result.deleteTables.length > 0) {
            updatedTables = tables.filter(t => !result.deleteTables.includes(t.name))
            message = `Deleted ${result.deleteTables.length} table(s)`
          }
          break

        case "edit":
          // Update existing tables
          result.tables.forEach(newTable => {
            const index = updatedTables.findIndex(t => t.name === newTable.name)
            if (index !== -1) {
              updatedTables[index] = {
                ...newTable,
                position: updatedTables[index].position // Keep existing position
              }
            }
          })
          message = `Updated ${result.tables.length} table(s)`
          break

        case "replace":
          // Replace entire schema
          updatedTables = result.tables
          message = `Replaced schema with ${result.tables.length} table(s)`
          break

        default:
          updatedTables = [...tables, ...result.tables]
          message = "Schema updated"
      }

      setTables(updatedTables)
      toast.success(message + (result.explanation ? `: ${result.explanation}` : ""))
      onClose()
    } catch (error) {
      console.error(error)
      toast.error(error.message || "Failed to generate schema")
    } finally {
      setIsGenerating(false)
    }
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
        className="glass-strong rounded-2xl w-full max-w-lg overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary/20 to-accent/20 flex items-center justify-center">
              <Sparkles className="w-4 h-4 text-primary" />
            </div>
            <h2 className="text-lg font-semibold">AI Schema Generator</h2>
          </div>
          <button onClick={onClose} className="p-2 rounded-lg hover:bg-secondary/50 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          <p className="text-sm text-muted-foreground mb-4">
            Describe what you want to do with your database schema. I can create new tables, edit existing ones, or delete tables.
          </p>

          <Textarea
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="Examples:
• Create tables for an e-commerce platform with users, products, and orders
• Add a comments table with user_id and post_id foreign keys
• Delete the old_users table
• Modify the products table to include a discount_price column
• Replace everything with a simple blog schema"
            rows={6}
            className="bg-input/50 resize-none mb-4"
          />

          <div className="text-xs text-muted-foreground space-y-1">
            <div><strong>Current tables:</strong> {tables.length === 0 ? "None" : tables.map(t => t.name).join(", ")}</div>
            <div><strong>Actions:</strong> Create new, Edit existing, Delete, or Replace entire schema</div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-border">
          <Button variant="outline" onClick={onClose} disabled={isGenerating}>
            Cancel
          </Button>
          <Button
            onClick={handleGenerate}
            disabled={isGenerating || !prompt.trim()}
            className="bg-gradient-to-r from-primary to-accent text-primary-foreground"
          >
            {isGenerating ? (
              <span className="flex items-center gap-2">
                <Loader2 className="w-4 h-4 animate-spin" />
                Generating...
              </span>
            ) : (
              <span className="flex items-center gap-2">
                <Sparkles className="w-4 h-4" />
                Generate with AI
              </span>
            )}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  )
}
