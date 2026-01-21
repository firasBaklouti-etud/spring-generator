"use client"

import { useState } from "react"
import { motion, AnimatePresence } from "framer-motion"
import {
    Database,
    ChevronDown,
    LayoutGrid,
    Check,
    Server,
    ArrowRight
} from "lucide-react"
import { Button } from "@/components/ui/button"
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogFooter,
} from "@/components/ui/dialog"
import { cn } from "@/lib/utils"
import { useGeneratorStore } from "@/lib/store"

interface DatabaseOption {
    id: string
    name: string
    type: "transactional" | "analytical"
    color: string
}

const DATABASES: DatabaseOption[] = [
    { id: "mysql", name: "MySQL", type: "transactional", color: "from-blue-400 to-cyan-400" },
    { id: "postgresql", name: "PostgreSQL", type: "transactional", color: "from-blue-500 to-indigo-500" },
    { id: "mariadb", name: "MariaDB", type: "transactional", color: "from-orange-400 to-yellow-400" },
    { id: "sqlite", name: "SQLite", type: "transactional", color: "from-blue-300 to-cyan-300" },
    { id: "sqlserver", name: "SQL Server", type: "transactional", color: "from-red-500 to-rose-500" },
]

interface DatabaseSelectionDialogProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    onContinue: () => void
}

export function DatabaseSelectionDialog({
    open,
    onOpenChange,
    onContinue
}: DatabaseSelectionDialogProps) {
    const { sqlDialect, setSqlDialect } = useGeneratorStore()
    const [activeTab, setActiveTab] = useState<"transactional" | "analytical">("transactional")
    const [showMore, setShowMore] = useState(false)

    const handleSelect = (id: string) => {
        setSqlDialect(id)
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-4xl max-h-[95vh] p-0 gap-0 overflow-hidden bg-background/95 backdrop-blur-xl border-border/50 flex flex-col">
                <div className="p-6 md:p-8 max-h-[70vh] overflow-y-auto">
                    <DialogHeader className="mb-8 text-center">
                        <DialogTitle className="text-2xl font-bold mb-2">What is your Database?</DialogTitle>
                        <DialogDescription className="text-muted-foreground text-base">
                            Each database has its own unique features and capabilities.
                        </DialogDescription>
                    </DialogHeader>

                    {/* Tabs */}
                    <div className="flex justify-center mb-8">
                        <div className="bg-muted/50 p-1 rounded-full flex gap-1">
                            <button
                                onClick={() => setActiveTab("transactional")}
                                className={cn(
                                    "px-6 py-2 rounded-full text-sm font-medium transition-all duration-200",
                                    activeTab === "transactional"
                                        ? "bg-primary text-primary-foreground shadow-lg"
                                        : "text-muted-foreground hover:text-foreground hover:bg-muted"
                                )}
                            >
                                Transactional
                            </button>
                            <button
                                onClick={() => setActiveTab("analytical")}
                                className={cn(
                                    "px-6 py-2 rounded-full text-sm font-medium transition-all duration-200",
                                    activeTab === "analytical"
                                        ? "bg-primary text-primary-foreground shadow-lg"
                                        : "text-muted-foreground hover:text-foreground hover:bg-muted"
                                )}
                            >
                                Analytical
                            </button>
                        </div>
                    </div>

                    {/* Grid */}
                    <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-8">
                        {DATABASES.filter(db => db.type === activeTab).map((db) => {
                            const isSelected = sqlDialect === db.id
                            return (
                                <motion.button
                                    key={db.id}
                                    onClick={() => handleSelect(db.id)}
                                    whileHover={{ scale: 1.02 }}
                                    whileTap={{ scale: 0.98 }}
                                    className={cn(
                                        "relative group flex flex-col items-center justify-center p-6 rounded-2xl border-2 transition-all duration-200 h-40",
                                        isSelected
                                            ? "border-primary bg-primary/5"
                                            : "border-border/50 bg-card hover:border-primary/50 hover:bg-accent/50"
                                    )}
                                >
                                    {isSelected && (
                                        <div className="absolute top-3 right-3 w-6 h-6 bg-primary text-primary-foreground rounded-full flex items-center justify-center">
                                            <Check className="w-3.5 h-3.5" />
                                        </div>
                                    )}

                                    <div className={cn(
                                        "w-16 h-16 rounded-xl mb-4 bg-gradient-to-br flex items-center justify-center shadow-lg",
                                        db.color
                                    )}>
                                        <Database className="w-8 h-8 text-white" />
                                    </div>

                                    <span className={cn(
                                        "font-medium text-lg",
                                        isSelected ? "text-primary" : "text-foreground"
                                    )}>
                                        {db.name}
                                    </span>
                                </motion.button>
                            )
                        })}
                    </div>

                    {/* More Databases */}
                    <div className="flex flex-col items-center gap-4">
                        <button
                            onClick={() => setShowMore(!showMore)}
                            className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
                        >
                            <ChevronDown className={cn("w-4 h-4 transition-transform", showMore && "rotate-180")} />
                            More Databases
                        </button>

                        <Button variant="outline" className="w-full sm:w-auto h-12 px-8 rounded-xl border-dashed">
                            <LayoutGrid className="w-4 h-4 mr-2" />
                            See Examples
                        </Button>
                    </div>
                </div>

                {/* Footer */}
                <DialogFooter className="p-6 bg-muted/30 border-t border-border/50 flex flex-col sm:flex-row gap-3 sm:gap-0 sm:justify-between items-center">
                    <Button variant="ghost" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>

                    <div className="flex items-center gap-3 w-full sm:w-auto">
                        <Button variant="outline" className="w-full sm:w-auto">
                            Empty Database
                        </Button>
                        <Button
                            onClick={onContinue}
                            className="w-full sm:w-auto bg-gradient-to-r from-pink-500 to-rose-500 hover:from-pink-600 hover:to-rose-600 text-white shadow-lg shadow-pink-500/25"
                        >
                            Continue
                            <ArrowRight className="w-4 h-4 ml-2" />
                        </Button>
                    </div>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}
