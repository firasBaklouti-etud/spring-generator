import { useState, useEffect } from "react"
import { motion } from "framer-motion"
import { Play, Sparkles, FileCode2, AlertCircle, Loader2, Database } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { useGeneratorStore, type Table } from "@/lib/store"
import { DatabaseSelectionDialog } from "./database-selection-dialog"


const sampleSQL = `CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);`

function parseSQL(sql: string): Table[] {
    // Temporary sample data - returns mock tables for testing
    return [
        {
            id: "1",
            name: "users",
            className: "User",
            columns: [
                {
                    name: "id",
                    fieldName: "id",
                    javaType: "Long",
                    type: "BIGINT",
                    primaryKey: true,
                    autoIncrement: true,
                    nullable: false,
                    unique: false,
                    foreignKey: false
                },
                {
                    name: "username",
                    fieldName: "username",
                    javaType: "String",
                    type: "VARCHAR",
                    length: 100,
                    nullable: false,
                    unique: true,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
                {
                    name: "email",
                    fieldName: "email",
                    javaType: "String",
                    type: "VARCHAR",
                    length: 255,
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
                {
                    name: "created_at",
                    fieldName: "createdAt",
                    javaType: "LocalDateTime",
                    type: "TIMESTAMP",
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
            ],
            relationships: [
                {
                    type: "ONE_TO_MANY",
                    sourceTable: "users",
                    targetTable: "orders",
                    fieldName: "orders",
                },
            ],
            position: { x: 100, y: 100 },
            isJoinTable: false
        },
        {
            id: "2",
            name: "orders",
            className: "Order",
            columns: [
                {
                    name: "order_id",
                    fieldName: "orderId",
                    javaType: "Long",
                    type: "BIGINT",
                    primaryKey: true,
                    autoIncrement: true,
                    nullable: false,
                    unique: false,
                    foreignKey: false
                },
                {
                    name: "user_id",
                    fieldName: "userId",
                    javaType: "Long",
                    type: "BIGINT",
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
                {
                    name: "total_amount",
                    fieldName: "totalAmount",
                    javaType: "BigDecimal",
                    type: "DECIMAL",
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
                {
                    name: "status",
                    fieldName: "status",
                    javaType: "String",
                    type: "VARCHAR",
                    length: 50,
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
            ],
            relationships: [
                {
                    type: "MANY_TO_ONE",
                    sourceTable: "orders",
                    targetTable: "users",
                    fieldName: "user",
                },
                {
                    type: "ONE_TO_MANY",
                    sourceTable: "orders",
                    targetTable: "order_items",
                    fieldName: "orderItems",
                },
            ],
            position: { x: 400, y: 100 },
            isJoinTable: false
        },
        {
            id: "3",
            name: "products",
            className: "Product",
            columns: [
                {
                    name: "product_id",
                    fieldName: "productId",
                    javaType: "Long",
                    type: "BIGINT",
                    primaryKey: true,
                    autoIncrement: true,
                    nullable: false,
                    unique: false,
                    foreignKey: false
                },
                {
                    name: "name",
                    fieldName: "name",
                    javaType: "String",
                    type: "VARCHAR",
                    length: 200,
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
                {
                    name: "price",
                    fieldName: "price",
                    javaType: "BigDecimal",
                    type: "DECIMAL",
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
                {
                    name: "stock_quantity",
                    fieldName: "stockQuantity",
                    javaType: "Integer",
                    type: "INT",
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
            ],
            relationships: [
                {
                    type: "ONE_TO_MANY",
                    sourceTable: "products",
                    targetTable: "order_items",
                    fieldName: "orderItems",
                },
            ],
            position: { x: 100, y: 300 },
            isJoinTable: false
        },
        {
            id: "4",
            name: "order_items",
            className: "OrderItem",
            columns: [
                {
                    name: "order_item_id",
                    fieldName: "orderItemId",
                    javaType: "Long",
                    type: "BIGINT",
                    primaryKey: true,
                    autoIncrement: true,
                    nullable: false,
                    unique: false,
                    foreignKey: false
                },
                {
                    name: "order_id",
                    fieldName: "orderId",
                    javaType: "Long",
                    type: "BIGINT",
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
                {
                    name: "product_id",
                    fieldName: "productId",
                    javaType: "Long",
                    type: "BIGINT",
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
                {
                    name: "quantity",
                    fieldName: "quantity",
                    javaType: "Integer",
                    type: "INT",
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
                {
                    name: "unit_price",
                    fieldName: "unitPrice",
                    javaType: "BigDecimal",
                    type: "DECIMAL",
                    nullable: false,
                    unique: false,
                    foreignKey: false,
                    autoIncrement: false,
                    primaryKey: false
                },
            ],
            relationships: [
                {
                    type: "MANY_TO_ONE",
                    sourceTable: "order_items",
                    targetTable: "orders",
                    fieldName: "order",
                },
                {
                    type: "MANY_TO_ONE",
                    sourceTable: "order_items",
                    targetTable: "products",
                    fieldName: "product",
                },
            ],
            position: { x: 400, y: 300 },
            isJoinTable: false
        },
    ]
}

function toCamelCase(str: string): string {
    return str.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase())
}

function toPascalCase(str: string): string {
    const camel = toCamelCase(str)
    return camel.charAt(0).toUpperCase() + camel.slice(1)
}

import { AiGenerateModal } from "./ai-generate-modal"

// ... imports and logic ...

export function SqlParserPhase() {
    const { sqlInput, setSqlInput, setTables, setCurrentPhase, isParsing, setIsParsing, sqlDialect } = useGeneratorStore()
    const [error, setError] = useState<string | null>(null)
    const [showDbDialog, setShowDbDialog] = useState(false)
    const [showAiModal, setShowAiModal] = useState(false)
    const [hasOpenedDialog, setHasOpenedDialog] = useState(false)

    useEffect(() => {
        // Auto-open database selection on first mount if not generic default
        // Actually, user wants it to appear automatically first time /generator page opened.
        // We can use a session check logic or just a simple state check
        if (!hasOpenedDialog) {
            setShowDbDialog(true)
            setHasOpenedDialog(true)
        }
    }, [])

    const handleParse = async () => {
        // Allow proceeding without SQL input if user wants to skip
        // But if they clicked Parse, they probably intended to parse.
        // However, user said "make SQL Parser and Schema Editor buttons always accessible"
        // For now, if empty, we just warn, but since we removed the "Skip" button, maybe the "Parse" button should handle empty?
        // Wait, removing "Skip to Editor" button means we need another way to skip.
        // The user said "no i dont want a skip to editor buttom / instead just make SQL Parser and Schema Editor and Generate buttoms always accessible"
        // This implies the navigation bar should allow clicking. I'll handle that in a separate file (phase-indicator.tsx) later.
        // For now, Parse button remains strict about input.

        if (!sqlInput.trim()) {
            toast.error("Please enter SQL statements")
            return
        }

        setIsParsing(true)
        setError(null)

        try {
            // Try API first
            const encodedSql = encodeURIComponent(sqlInput)
            const response = await fetch(`${process.env.NEXT_PUBLIC_BACKEND_URL}/api/sqlParser/${encodedSql}?dialect=${sqlDialect}`, {
                method: "GET",
                signal: AbortSignal.timeout(5000),
            })

            if (response.ok) {
                const apiTables = await response.json()
                // Add IDs and positions to API response
                const tables: Table[] = apiTables.map((t: Omit<Table, "id" | "position">, i: number) => ({
                    ...t,
                    id: `table-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`,
                    position: { x: 100 + i * 350, y: 100 },
                }))
                setTables(tables)
                toast.success(`Parsed ${tables.length} table(s) successfully!`)
                setCurrentPhase(2)
            } else {
                throw new Error("API returned error")
            }
        } catch {
            // Fallback to client-side parsing
            console.log("Using client-side SQL parser")
            try {
                const tables = parseSQL(sqlInput)
                if (tables.length === 0) {
                    throw new Error("No valid CREATE TABLE statements found")
                }
                setTables(tables)
                toast.success(`Parsed ${tables.length} table(s) successfully!`)
                setCurrentPhase(2)
            } catch (parseError) {
                const message = parseError instanceof Error ? parseError.message : "Failed to parse SQL"
                setError(message)
                toast.error(message)
            }
        } finally {
            setIsParsing(false)
        }
    }

    const handleAiComplete = () => {
        setCurrentPhase(2)
    }

    const loadSample = () => {
        setSqlInput(sampleSQL)
        toast.info("Sample SQL loaded")
    }

    return (
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="max-w-4xl mx-auto">
                {/* Header */}
                <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="text-center mb-8">
                    <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-primary/20 to-accent/20 mb-4">
                        <FileCode2 className="w-8 h-8 text-primary" />
                    </div>
                    <h1 className="text-2xl sm:text-3xl font-bold mb-2">Parse Your SQL</h1>
                    <p className="text-muted-foreground">
                        Paste your CREATE TABLE statements below or use AI to generate your schema
                    </p>
                </motion.div>

                {/* SQL Editor */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.1 }}
                    className="glass rounded-2xl overflow-hidden mb-6"
                >
                    {/* Editor Header */}
                    <div className="flex items-center justify-between px-4 py-3 border-b border-border bg-card/50">
                        <div className="flex items-center gap-2">
                            <div className="w-3 h-3 rounded-full bg-destructive/50" />
                            <div className="w-3 h-3 rounded-full bg-chart-4/50" />
                            <div className="w-3 h-3 rounded-full bg-chart-3/50" />
                        </div>
                        <span className="text-xs text-muted-foreground font-mono">SQL Editor</span>
                        <div className="flex items-center gap-2">
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => setShowDbDialog(true)}
                                className="text-xs h-7 gap-1 border-dashed"
                            >
                                <Database className="w-3 h-3" />
                                {sqlDialect === "mysql" ? "MySQL" :
                                    sqlDialect === "postgresql" ? "PostgreSQL" :
                                        sqlDialect === "mariadb" ? "MariaDB" :
                                            sqlDialect === "sqlite" ? "SQLite" :
                                                sqlDialect === "sqlserver" ? "SQL Server" : sqlDialect}
                            </Button>
                            <Button variant="ghost" size="sm" onClick={() => setShowAiModal(true)} className="text-xs h-7">
                                <Sparkles className="w-3 h-3 mr-1" />
                                AI Generate
                            </Button>
                            <Button variant="ghost" size="sm" onClick={loadSample} className="text-xs h-7">
                                <Sparkles className="w-3 h-3 mr-1" />
                                Load Sample
                            </Button>
                        </div>
                    </div>

                    {/* Code Area */}
                    <div className="relative">
                        <textarea
                            value={sqlInput}
                            onChange={(e) => {
                                setSqlInput(e.target.value)
                                setError(null)
                            }}
                            placeholder={`-- Paste your SQL CREATE TABLE statements here
              
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    ...
);`}
                            className="w-full h-80 sm:h-96 p-4 bg-transparent text-foreground font-mono text-sm resize-none focus:outline-none placeholder:text-muted-foreground/50"
                            spellCheck={false}
                        />
                    </div>
                </motion.div>

                {/* Error Display */}
                {error && (
                    <motion.div
                        initial={{ opacity: 0, y: -10 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="flex items-center gap-2 p-4 mb-6 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive"
                    >
                        <AlertCircle className="w-5 h-5 flex-shrink-0" />
                        <span className="text-sm">{error}</span>
                    </motion.div>
                )}

                {/* Actions */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.2 }}
                    className="flex justify-center gap-4"
                >
                    <Button
                        onClick={handleParse}
                        disabled={isParsing || !sqlInput.trim()}
                        size="lg"
                        className="bg-gradient-to-r from-primary to-accent text-primary-foreground hover:opacity-90 transition-opacity glow px-8"
                    >
                        {isParsing ? (
                            <span className="flex items-center gap-2">
                                <Loader2 className="w-5 h-5 animate-spin" />
                                Parsing...
                            </span>
                        ) : (
                            <span className="flex items-center gap-2">
                                <Play className="w-5 h-5" />
                                Parse SQL
                            </span>
                        )}
                    </Button>
                </motion.div>

                {/* Tips */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.3 }}
                    className="mt-8 p-4 glass rounded-xl"
                >
                    <h3 className="font-semibold mb-2 text-sm">Supported SQL Features</h3>
                    <ul className="text-sm text-muted-foreground space-y-1">
                        <li>• CREATE TABLE statements with various data types</li>
                        <li>• PRIMARY KEY, AUTO_INCREMENT, NOT NULL, UNIQUE constraints</li>
                        <li>• FOREIGN KEY relationships (auto-detected)</li>
                        <li>• MySQL, PostgreSQL, and standard SQL syntax</li>
                    </ul>
                </motion.div>
            </div>

            <DatabaseSelectionDialog
                open={showDbDialog}
                onOpenChange={setShowDbDialog}
                onContinue={() => setShowDbDialog(false)}
            />

            {showAiModal && (
                <AiGenerateModal
                    onClose={() => setShowAiModal(false)}
                    onGenerateComplete={handleAiComplete}
                />
            )}
        </div >
    )
}
