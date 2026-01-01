"use client"

import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
import {
    Shield,
    Lock,
    Unlock,
    Key,
    Database,
    Search,
    AlertTriangle,
    CheckCircle2,
    ListFilter,
    ArrowRight,
    Plus,
    X,
    Check,
    User,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useGeneratorStore } from "@/lib/store"
import { toast } from "sonner"

const springMethods = ["GET", "POST", "PUT", "DELETE", "PATCH", "ALL"]
const accessLevels = ["PERMIT_ALL", "AUTHENTICATED", "HAS_ROLE"]

export function SecurityPhase() {
    const { tables, projectConfig, setSecurityConfig, addSecurityRule, removeSecurityRule } = useGeneratorStore()
    const [activeTab, setActiveTab] = useState("resources")
    const [searchTerm, setSearchTerm] = useState("")

    // Local state for UI responsiveness before syncing to store if needed, 
    // or just use store directly. Using store.

    const security = projectConfig.securityConfig || { enabled: true, authenticationType: "BASIC", useDbAuth: false, rules: [] }
    const rules = security.rules || []
    const rbacMode = security.rbacMode || "STATIC"
    const definedRoles = security.definedRoles || []
    const availablePermissions = security.permissions || []

    // 1. Initialize Defaults (Roles & Permissions)
    useEffect(() => {
        if (projectConfig.stackType === "SPRING") {
            // Ensure security is enabled
            if (!projectConfig.securityConfig?.enabled) {
                setSecurityConfig({ enabled: true })
            }

            // Sync Permissions with Tables (Auto-generate)
            const generatedPermissions = tables.flatMap(t => [
                `${t.className.toUpperCase()}_READ`,
                `${t.className.toUpperCase()}_WRITE`,
                `${t.className.toUpperCase()}_DELETE`
            ])

            // Merge with existing to avoid duplicates
            const currentPerms = new Set(security.permissions || [])
            let hasNew = false
            generatedPermissions.forEach(p => {
                if (!currentPerms.has(p)) {
                    currentPerms.add(p)
                    hasNew = true
                }
            })

            // Initialize Roles if empty
            let newRoles = [...(security.definedRoles || [])]
            if (newRoles.length === 0) {
                newRoles = [
                    { name: "USER", permissions: [] },
                    { name: "ADMIN", permissions: Array.from(currentPerms) } // Admin gets all by default
                ]
                hasNew = true
            }

            if (hasNew) {
                setSecurityConfig({
                    permissions: Array.from(currentPerms).sort(),
                    definedRoles: newRoles
                })
            }
        }
    }, [projectConfig.stackType, tables.length]) // Re-run when tables change to update permissions

    // Helper: Toggle Permission for a Role
    const toggleRolePermission = (roleName: string, permission: string) => {
        const updatedRoles = definedRoles.map(r => {
            if (r.name === roleName) {
                const hasPerm = r.permissions.includes(permission)
                return {
                    ...r,
                    permissions: hasPerm
                        ? r.permissions.filter(p => p !== permission)
                        : [...r.permissions, permission]
                }
            }
            return r
        })
        setSecurityConfig({ definedRoles: updatedRoles })
    }

    const addRole = () => {
        const name = prompt("Enter Role Name (e.g. MANAGER):")
        if (name) {
            const cleanName = name.toUpperCase().replace(/[^A-Z0-9_]/g, "_")
            if (definedRoles.find(r => r.name === cleanName)) return
            setSecurityConfig({ definedRoles: [...definedRoles, { name: cleanName, permissions: [] }] })
        }
    }

    // Helper to find rule for a table endpoint
    const getRuleForEntity = (entityName: string, method: string) => {
        const path = `/api/${entityName.toLowerCase()}/**`
        return rules.find((r) => r.path === path && r.method === method)
    }

    // Update rule for entity
    const handleEntityRuleChange = (entityName: string, method: string, level: string, role?: string) => {
        const path = `/api/${entityName.toLowerCase()}/**`
        const existingRule = rules.find((r) => r.path === path && r.method === method)

        const newRule = {
            path,
            method,
            rule: level,
            role: role || (level === "HAS_ROLE" ? "ADMIN" : undefined)
        }

        if (existingRule) {
            removeSecurityRule(path, method)
            if (level !== "DEFAULT") addSecurityRule(newRule)
        } else {
            if (level !== "DEFAULT") addSecurityRule(newRule)
        }
    }

    const filteredTables = tables.filter(t => t.name.toLowerCase().includes(searchTerm.toLowerCase()))

    return (
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="max-w-5xl mx-auto">

                {/* Header */}
                <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="text-center mb-10">
                    <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-red-500/20 to-orange-500/20 mb-4 ring-1 ring-red-500/30">
                        <Shield className="w-8 h-8 text-red-500" />
                    </div>
                    <h1 className="text-3xl font-bold mb-2">Secure Your API</h1>
                    <p className="text-muted-foreground max-w-lg mx-auto">
                        Configure Authentication, RBAC Modes, and granular Permissions.
                    </p>
                </motion.div>

                {/* Global Config Card */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
                    className="glass rounded-2xl p-6 mb-8 border-l-4 border-l-primary"
                >
                    <div className="flex flex-wrap items-center justify-between gap-6">
                        <div className="flex items-center gap-4">
                            <div className="p-3 rounded-full bg-secondary/50">
                                <Key className="w-6 h-6 text-primary" />
                            </div>
                            <div>
                                <h3 className="font-semibold text-lg">Authentication Strategy</h3>
                                <p className="text-sm text-muted-foreground">How will users prove their identity?</p>
                            </div>
                        </div>
                        <Select
                            value={security.authenticationType}
                            onValueChange={(value: any) => setSecurityConfig({ authenticationType: value })}
                        >
                            <SelectTrigger className="w-[200px] h-12 bg-background/50 border-input/50">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="BASIC">Basic Auth (Simple)</SelectItem>
                                <SelectItem value="JWT">JWT (Stateless Token)</SelectItem>
                                <SelectItem value="OAUTH2">OAuth2 / Social Login</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>
                </motion.div>

                {/* Identity & RBAC Config */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}
                    className="glass rounded-2xl p-6 mb-8 border-l-4 border-l-blue-500"
                >
                    <div className="flex flex-col gap-8">
                        {/* Header Section */}
                        <div className="flex items-center gap-4">
                            <div className="p-3 rounded-full bg-blue-500/20">
                                <User className="w-6 h-6 text-blue-500" />
                            </div>
                            <div>
                                <h3 className="font-semibold text-lg">Identity & RBAC Model</h3>
                                <p className="text-sm text-muted-foreground">Define your User entity and Authorization architecture.</p>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                            {/* Principal Entity */}
                            <div className="space-y-4">
                                <Label>Principal Entity (User Table)</Label>
                                <Select
                                    value={security.principalEntity}
                                    onValueChange={(value) => setSecurityConfig({ principalEntity: value, usernameField: undefined, passwordField: undefined })}
                                >
                                    <SelectTrigger className="bg-background/50">
                                        <SelectValue placeholder="Select User Entity" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {tables.map(t => (
                                            <SelectItem key={t.id} value={t.className}>{t.className}</SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>

                                {security.principalEntity && (
                                    <div className="grid grid-cols-2 gap-4 animate-in fade-in slide-in-from-top-2">
                                        <div className="space-y-2">
                                            <Label className="text-xs text-muted-foreground">Username Field</Label>
                                            <Select
                                                value={security.usernameField}
                                                onValueChange={(value) => setSecurityConfig({ usernameField: value })}
                                            >
                                                <SelectTrigger className="h-9 text-xs bg-background/50">
                                                    <SelectValue placeholder="e.g. email" />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    {tables.find(t => t.className === security.principalEntity)?.columns.map(c => (
                                                        <SelectItem key={c.name} value={c.fieldName}>{c.fieldName}</SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        </div>
                                        <div className="space-y-2">
                                            <Label className="text-xs text-muted-foreground">Password Field</Label>
                                            <Select
                                                value={security.passwordField}
                                                onValueChange={(value) => setSecurityConfig({ passwordField: value })}
                                            >
                                                <SelectTrigger className="h-9 text-xs bg-background/50">
                                                    <SelectValue placeholder="e.g. password" />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    {tables.find(t => t.className === security.principalEntity)?.columns.map(c => (
                                                        <SelectItem key={c.name} value={c.fieldName}>{c.fieldName}</SelectItem>
                                                    ))}
                                                    <SelectItem value="password">password (Inject)</SelectItem>
                                                </SelectContent>
                                            </Select>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* RBAC Mode Strategy */}
                            <div className="space-y-4">
                                <Label>RBAC Mode</Label>
                                <div className="flex bg-black/20 p-1 rounded-lg">
                                    <button
                                        onClick={() => setSecurityConfig({ rbacMode: "STATIC" })}
                                        className={`flex-1 py-2 text-sm rounded-md transition-all ${rbacMode === 'STATIC' ? 'bg-primary text-primary-foreground shadow' : 'hover:bg-white/5 opacity-70'}`}
                                    >
                                        Static (Enums)
                                        <span className="block text-[10px] opacity-70 font-normal">Fast, Simple, No DB</span>
                                    </button>
                                    <button
                                        onClick={() => setSecurityConfig({ rbacMode: "DYNAMIC" })}
                                        className={`flex-1 py-2 text-sm rounded-md transition-all ${rbacMode === 'DYNAMIC' ? 'bg-primary text-primary-foreground shadow' : 'hover:bg-white/5 opacity-70'}`}
                                    >
                                        Dynamic (Database)
                                        <span className="block text-[10px] opacity-70 font-normal">Flexible, Admin Dashboard</span>
                                    </button>
                                </div>

                                {rbacMode === "DYNAMIC" && (
                                    <div className="animate-in fade-in slide-in-from-top-2">
                                        <Label className="text-xs text-muted-foreground mb-2 block">Role Entity (Optional Override)</Label>
                                        <Select
                                            value={security.roleEntity || "AUTO"}
                                            onValueChange={(value) => setSecurityConfig({ roleEntity: value === "AUTO" ? undefined : value })}
                                        >
                                            <SelectTrigger className="bg-background/50">
                                                <SelectValue placeholder="Auto-Generate Role Identity" />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem value="AUTO">Auto-Generate (Recommended)</SelectItem>
                                                {tables.filter(t => t.className !== security.principalEntity).map(t => (
                                                    <SelectItem key={t.id} value={t.className}>{t.className}</SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Roles & Permissions Editor */}
                        <div className="border-t border-white/10 pt-6">
                            <h4 className="text-sm font-semibold mb-4 flex items-center gap-2">
                                <ListFilter className="w-4 h-4 text-primary" /> Role & Permission Definitions
                            </h4>

                            <Tabs defaultValue="roles" className="w-full">
                                <TabsList className="w-full justify-start bg-transparent border-b border-white/10 rounded-none h-10 p-0 mb-4">
                                    <TabsTrigger value="roles" className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent px-4">Roles</TabsTrigger>
                                    <TabsTrigger value="permissions" className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent px-4">Permissions ({availablePermissions.length})</TabsTrigger>
                                </TabsList>

                                <TabsContent value="roles" className="space-y-4">
                                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                                        {definedRoles.map(role => (
                                            <div key={role.name} className="bg-black/20 p-4 rounded-xl border border-white/5">
                                                <div className="flex items-center justify-between mb-3">
                                                    <div className="flex items-center gap-2">
                                                        <Badge variant="outline" className="font-mono">{role.name}</Badge>
                                                        <span className="text-xs text-muted-foreground">{role.permissions.length} permissions</span>
                                                    </div>
                                                </div>
                                                <div className="h-40 overflow-y-auto pr-2 space-y-1 custom-scrollbar">
                                                    {availablePermissions.map(perm => (
                                                        <label key={perm} className="flex items-center gap-2 text-xs p-1.5 hover:bg-white/5 rounded cursor-pointer">
                                                            <input
                                                                type="checkbox"
                                                                checked={role.permissions.includes(perm)}
                                                                onChange={() => toggleRolePermission(role.name, perm)}
                                                                className="rounded border-white/20 bg-black/20 h-3 w-3 text-primary"
                                                            />
                                                            <span className={role.permissions.includes(perm) ? "text-primary" : "text-muted-foreground"}>{perm}</span>
                                                        </label>
                                                    ))}
                                                </div>
                                            </div>
                                        ))}
                                        <button onClick={addRole} className="h-full min-h-[160px] border-2 border-dashed border-white/10 rounded-xl flex flex-col items-center justify-center text-muted-foreground hover:bg-white/5 hover:border-primary/30 transition-all gap-2">
                                            <Plus className="w-6 h-6" />
                                            <span>Add Role</span>
                                        </button>
                                    </div>
                                </TabsContent>

                                <TabsContent value="permissions" className="space-y-4">
                                    <div className="bg-black/20 p-4 rounded-xl border border-white/5">
                                        <div className="flex flex-wrap gap-2">
                                            {availablePermissions.map(perm => (
                                                <Badge key={perm} variant="secondary" className="font-mono bg-white/5 hover:bg-white/10">
                                                    {perm}
                                                </Badge>
                                            ))}
                                            <Button variant="outline" size="sm" className="h-6 text-xs border-dashed" onClick={() => {
                                                const p = prompt("Enter Custom Permission (e.g. REPORT_EXPORT):");
                                                if (p) {
                                                    const clean = p.toUpperCase().replace(/[^A-Z0-9_]/g, "_");
                                                    if (!availablePermissions.includes(clean)) setSecurityConfig({ permissions: [...availablePermissions, clean].sort() })
                                                }
                                            }}>
                                                <Plus className="w-3 h-3 mr-1" /> Add
                                            </Button>
                                        </div>
                                    </div>
                                </TabsContent>
                            </Tabs>
                        </div>
                    </div>
                </motion.div>

                {/* Resource Permissions */}
                <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
                    <TabsList className="grid w-full grid-cols-1 mb-8 bg-black/20 p-1 rounded-xl h-14">
                        <TabsTrigger value="resources" className="rounded-lg h-12 data-[state=active]:bg-primary/20 data-[state=active]:text-primary font-medium text-base">
                            <Database className="w-4 h-4 mr-2" /> Resource Permissions (By Endpoint)
                        </TabsTrigger>
                    </TabsList>

                    <TabsContent value="resources" className="space-y-6">
                        {/* Search Bar */}
                        <div className="relative mb-6">
                            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground w-4 h-4" />
                            <Input
                                placeholder="Search entities..."
                                className="pl-10 h-12 bg-black/10 border-white/10"
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 gap-6">
                            {filteredTables.map((table) => (
                                <motion.div
                                    key={table.id}
                                    initial={{ opacity: 0, scale: 0.95 }}
                                    animate={{ opacity: 1, scale: 1 }}
                                    className="glass rounded-xl overflow-hidden border border-white/5 hover:border-primary/20 transition-all duration-300"
                                >
                                    <div className="p-4 bg-white/5 border-b border-white/5 flex items-center justify-between">
                                        <h3 className="font-bold flex items-center gap-2">
                                            <div className="w-2 h-2 rounded-full bg-primary" />
                                            {table.className}
                                        </h3>
                                        <Badge variant="outline" className="opacity-50 text-xs">/api/{table.className.toLowerCase()}</Badge>
                                    </div>

                                    <div className="p-4 space-y-3">
                                        {["GET", "POST", "PUT", "DELETE"].map((method) => {
                                            const rule = getRuleForEntity(table.className, method)
                                            const currentLevel = rule?.rule || "AUTHENTICATED"

                                            return (
                                                <div key={method} className="flex items-center justify-between group">
                                                    <div className="flex items-center gap-3 w-20">
                                                        <Badge variant="secondary" className="w-16 justify-center font-mono opacity-70 group-hover:opacity-100 transition-opacity">
                                                            {method}
                                                        </Badge>
                                                    </div>

                                                    <div className="flex bg-black/20 rounded-lg p-1 gap-1 flex-wrap">
                                                        {/* Public Access */}
                                                        <button
                                                            onClick={() => handleEntityRuleChange(table.className, method, "PERMIT_ALL")}
                                                            className={`px-3 py-1 text-xs rounded-md transition-all ${currentLevel === 'PERMIT_ALL' ? 'bg-green-500 text-white shadow-lg' : 'text-muted-foreground hover:bg-white/5'}`}
                                                        >
                                                            Public
                                                        </button>
                                                        {/* Authenticated */}
                                                        <button
                                                            onClick={() => handleEntityRuleChange(table.className, method, "AUTHENTICATED")}
                                                            className={`px-3 py-1 text-xs rounded-md transition-all ${currentLevel === 'AUTHENTICATED' ? 'bg-blue-500 text-white shadow-lg' : 'text-muted-foreground hover:bg-white/5'}`}
                                                        >
                                                            Auth
                                                        </button>
                                                        {/* Dynamic Roles */}
                                                        {definedRoles.map((role, idx) => {
                                                            const isActive = currentLevel === 'HAS_ROLE' && rule?.role === role.name
                                                            const colors = ['bg-red-500', 'bg-purple-500', 'bg-orange-500', 'bg-pink-500']
                                                            const activeColor = colors[idx % colors.length]
                                                            return (
                                                                <button
                                                                    key={role.name}
                                                                    onClick={() => handleEntityRuleChange(table.className, method, "HAS_ROLE", role.name)}
                                                                    className={`px-3 py-1 text-xs rounded-md transition-all ${isActive ? `${activeColor} text-white shadow-lg` : 'text-muted-foreground hover:bg-white/5'}`}
                                                                >
                                                                    {role.name}
                                                                </button>
                                                            )
                                                        })}
                                                    </div>
                                                </div>
                                            )
                                        })}
                                    </div>
                                </motion.div>
                            ))}
                        </div>
                    </TabsContent>
                </Tabs>

                <div className="flex justify-between mt-8">
                    <Button variant="ghost" onClick={() => { useGeneratorStore.getState().setCurrentPhase(2) }}>
                        Back
                    </Button>
                    <Button
                        onClick={() => { useGeneratorStore.getState().setCurrentPhase(4) }}
                        className="bg-gradient-to-r from-primary to-accent text-primary-foreground hover:opacity-90"
                    >
                        Continue to Project Settings
                        <ArrowRight className="w-4 h-4 ml-1" />
                    </Button>
                </div>
            </div>
        </div >
    )
}
