"use client"

import { useState, useMemo, useEffect } from "react"
import { motion } from "framer-motion"
import { X, Search, Check, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import type { DependencyGroup, StackType } from "@/lib/store"


function getCategoryColor(groupName: string): string {
  const colors: Record<string, string> = {
    "Developer Tools": "bg-emerald-500/20 text-emerald-400",
    Web: "bg-blue-500/20 text-blue-400",
    "Web Framework": "bg-blue-500/20 text-blue-400",
    Security: "bg-red-500/20 text-red-400",
    SQL: "bg-amber-500/20 text-amber-400",
    NoSQL: "bg-purple-500/20 text-purple-400",
    Messaging: "bg-pink-500/20 text-pink-400",
    "I/O": "bg-cyan-500/20 text-cyan-400",
    Ops: "bg-orange-500/20 text-orange-400",
    Testing: "bg-teal-500/20 text-teal-400",
    Core: "bg-indigo-500/20 text-indigo-400",
    Database: "bg-amber-500/20 text-amber-400",
    Authentication: "bg-red-500/20 text-red-400",
    Validation: "bg-cyan-500/20 text-cyan-400",
    "API Documentation": "bg-violet-500/20 text-violet-400",
    "Background Tasks": "bg-orange-500/20 text-orange-400",
  }
  return colors[groupName] || "bg-gray-500/20 text-gray-400"
}

interface DependenciesModalProps {
  selectedDependencies: string[]
  onSelect: (dependencies: string[]) => void
  onClose: () => void
  stackType?: StackType
}

export function DependenciesModal({
  selectedDependencies,
  onSelect,
  onClose,
  stackType = "SPRING",
}: DependenciesModalProps) {
  const [searchQuery, setSearchQuery] = useState("")
  const [localSelected, setLocalSelected] = useState<string[]>(selectedDependencies)
  const [dependencyGroups, setDependencyGroups] = useState<DependencyGroup[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Fetch dependency groups from backend
  useEffect(() => {
    const fetchDependencyGroups = async () => {
      try {
        setIsLoading(true)
        setError(null)

        const response = await fetch(`${process.env.NEXT_PUBLIC_BACKEND_URL}/api/dependencies/groups?stackType=${stackType}`)

        if (!response.ok) {
          throw new Error(`Failed to fetch dependencies: ${response.status}`)
        }

        const data = await response.json()
        setDependencyGroups(data)
      } catch (err) {
        console.error("Error fetching dependency groups:", err)
        setError("Failed to load dependencies. Using fallback data.")

        // Fallback to legacy data based on stack type
        switch (stackType) {
          case "SPRING":
            setDependencyGroups(SPRING_DEPENDENCY_GROUPS)
            break
          case "NODE":
            // You would add NODE_DEPENDENCY_GROUPS here
            setDependencyGroups([])
            break
          case "NEST":
            // You would add NEST_DEPENDENCY_GROUPS here
            setDependencyGroups([])
            break
          case "FASTAPI":
            // You would add FASTAPI_DEPENDENCY_GROUPS here
            setDependencyGroups([])
            break
          default:
            setDependencyGroups(SPRING_DEPENDENCY_GROUPS)
        }
      } finally {
        setIsLoading(false)
      }
    }

    fetchDependencyGroups()
  }, [stackType])

  const filteredGroups = useMemo(() => {
    if (!searchQuery.trim()) return dependencyGroups

    const query = searchQuery.toLowerCase()
    return dependencyGroups
      .map((group) => ({
        ...group,
        dependencies: group.dependencies.filter(
          (dep) =>
            dep.name.toLowerCase().includes(query) ||
            dep.description.toLowerCase().includes(query) ||
            dep.id.toLowerCase().includes(query),
        ),
      }))
      .filter((group) => group.dependencies.length > 0)
  }, [searchQuery, dependencyGroups])

  const toggleDependency = (depId: string) => {
    setLocalSelected((prev) => (prev.includes(depId) ? prev.filter((id) => id !== depId) : [...prev, depId]))
  }

  const handleAddSelected = () => {
    onSelect(localSelected)
    onClose()
  }

  const totalSelected = localSelected.length

  const stackName =
    stackType === "SPRING"
      ? "Spring Boot"
      : stackType === "NODE"
        ? "Node.js"
        : stackType === "NEST"
          ? "NestJS"
          : "FastAPI"

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
        className="glass-strong rounded-2xl w-full max-w-2xl max-h-[80vh] flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border">
          <div>
            <h2 className="text-xl font-bold">Add Dependencies</h2>
            <p className="text-sm text-muted-foreground">{stackName} packages</p>
          </div>
          <button onClick={onClose} className="p-2 rounded-lg hover:bg-secondary/50 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search Bar */}
        <div className="px-6 py-4 border-b border-border">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search dependencies by name or description..."
              className="pl-10 bg-input/50 border-primary/30 focus:border-primary"
              autoFocus
              disabled={isLoading}
            />
          </div>
        </div>

        {/* Dependencies List */}
        <div className="flex-1 overflow-y-auto px-6 py-4 space-y-6">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="w-8 h-8 animate-spin text-primary" />
              <span className="ml-3 text-muted-foreground">Loading dependencies...</span>
            </div>
          ) : error ? (
            <div className="text-center py-6">
              <div className="text-yellow-600 dark:text-yellow-400 mb-2">
                {error}
              </div>
              <div className="text-sm text-muted-foreground">
                Showing fallback data. Some features may be limited.
              </div>
            </div>
          ) : filteredGroups.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
              {searchQuery.trim()
                ? `No dependencies found matching "${searchQuery}"`
                : "No dependencies available for this stack type"
              }
            </div>
          ) : (
            filteredGroups.map((group) => (
              <div key={group.name}>
                {/* Group Header */}
                <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-3">
                  {group.name}
                </h3>

                {/* Dependencies */}
                <div className="space-y-2">
                  {group.dependencies.map((dep) => {
                    const isSelected = localSelected.includes(dep.id)
                    return (
                      <button
                        key={dep.id}
                        onClick={() => toggleDependency(dep.id)}
                        className={`w-full flex items-start gap-3 p-4 rounded-xl text-left transition-all ${isSelected
                          ? "bg-primary/10 border border-primary/30"
                          : "bg-secondary/20 border border-transparent hover:bg-secondary/40"
                          }`}
                      >
                        {/* Checkbox */}
                        <div
                          className={`flex-shrink-0 w-5 h-5 rounded border-2 flex items-center justify-center mt-0.5 transition-colors ${isSelected ? "bg-primary border-primary" : "border-muted-foreground/40"
                            }`}
                        >
                          {isSelected && <Check className="w-3 h-3 text-primary-foreground" />}
                        </div>

                        {/* Content */}
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-medium">{dep.name}</span>
                            <span
                              className={`px-2 py-0.5 text-[10px] font-semibold uppercase rounded ${getCategoryColor(group.name)}`}
                            >
                              {group.name}
                            </span>
                          </div>
                          <p className="text-sm text-muted-foreground mt-1 line-clamp-2">{dep.description}</p>
                        </div>
                      </button>
                    )
                  })}
                </div>
              </div>
            ))
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-border bg-card/50">
          <span className="text-sm text-muted-foreground">
            {totalSelected} dependenc{totalSelected === 1 ? "y" : "ies"} selected
          </span>
          <div className="flex gap-3">
            <Button variant="outline" onClick={onClose} className="glass bg-transparent">
              Cancel
            </Button>
            <Button
              onClick={handleAddSelected}
              disabled={isLoading}
              className="bg-gradient-to-r from-primary to-accent text-primary-foreground hover:opacity-90 disabled:opacity-50"
            >
              Add Selected
            </Button>
          </div>
        </div>
      </motion.div>
    </motion.div>
  )
}

// Legacy exports for backward compatibility
export { SPRING_DEPENDENCY_GROUPS as DEPENDENCY_GROUPS }