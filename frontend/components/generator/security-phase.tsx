"use client"

import { useState, useEffect, useMemo } from "react"
import { motion } from "framer-motion"
import {
    Shield,
    Lock,
    Key,
    Database,
    Search,
    ListFilter,
    ArrowRight,
    Plus,
    User,
    Globe,
    Settings,
    KeyRound,
    ServerCog,
    ShieldCheck,
    Fingerprint,
    RefreshCw,
    UserPlus,
    ToggleLeft,
    FileKey,
    Users,
    Github,
    Facebook,
    CheckCircle2,
    Circle,
    RotateCcw,
    FlaskConical,
    Layers,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import { useGeneratorStore } from "@/lib/store"

// ─── Constants ──────────────────────────────────────────────────────────────────

const AUTH_STRATEGIES = [
    {
        value: "JWT" as const,
        label: "JWT",
        description: "Stateless token-based auth",
        icon: KeyRound,
    },
    {
        value: "BASIC" as const,
        label: "Basic Auth",
        description: "Simple username & password",
        icon: Lock,
    },
    {
        value: "FORM_LOGIN" as const,
        label: "Form Login",
        description: "Server-side session with login form",
        icon: Fingerprint,
    },
    {
        value: "KEYCLOAK_RS" as const,
        label: "Keycloak RS",
        description: "Resource Server with Keycloak IdP",
        icon: ServerCog,
    },
    {
        value: "KEYCLOAK_OAUTH" as const,
        label: "Keycloak OAuth",
        description: "OAuth2 Client via Keycloak",
        icon: ShieldCheck,
    },
] as const

const SOCIAL_PROVIDERS = [
    { id: "GOOGLE", label: "Google", icon: Globe, color: "text-red-400" },
    { id: "GITHUB", label: "GitHub", icon: Github, color: "text-zinc-300" },
    { id: "FACEBOOK", label: "Facebook", icon: Facebook, color: "text-blue-400" },
] as const

const tabMotion = {
    initial: { opacity: 0, y: 8 },
    animate: { opacity: 1, y: 0, transition: { duration: 0.25, ease: "easeOut" } },
    exit: { opacity: 0, y: -8, transition: { duration: 0.15 } },
}

// ─── Component ──────────────────────────────────────────────────────────────────

export function SecurityPhase() {
    const { tables, projectConfig, setSecurityConfig, addSecurityRule, removeSecurityRule } = useGeneratorStore()

    const [activeTab, setActiveTab] = useState("identity")
    const [searchTerm, setSearchTerm] = useState("")

    const security = projectConfig.securityConfig || {
        enabled: true,
        authenticationType: "JWT",
        useDbAuth: false,
        rules: [],
    }

    const rules = security.rules || []
    const rbacMode = security.rbacMode || "STATIC"
    const definedRoles = security.definedRoles || []
    const availablePermissions = security.permissions || []
    const authType = security.authenticationType || "JWT"

    const isJwt = authType === "JWT"
    const isKeycloak = authType === "KEYCLOAK_RS" || authType === "KEYCLOAK_OAUTH"

    // Compute visible tabs based on auth type
    const visibleTabs = useMemo(() => {
        const base = [
            { value: "identity", label: "Identity", icon: User },
        ]
        if (isJwt) {
            base.push({ value: "token", label: "Token & Session", icon: RefreshCw })
            base.push({ value: "social", label: "Social Logins", icon: Globe })
        }
        if (isKeycloak) {
            base.push({ value: "keycloak", label: "Keycloak", icon: ServerCog })
        }
        base.push(
            { value: "roles", label: "Roles & Permissions", icon: Users },
            { value: "endpoints", label: "Endpoints", icon: Database },
            { value: "advanced", label: "Advanced", icon: Settings },
        )
        return base
    }, [isJwt, isKeycloak])

    // Reset to a valid tab if current tab is hidden
    useEffect(() => {
        if (!visibleTabs.find(t => t.value === activeTab)) {
            setActiveTab("identity")
        }
    }, [visibleTabs, activeTab])

    // ─── Initialize roles & permissions from tables ────────────────────────────
    useEffect(() => {
        if (projectConfig.stackType === "SPRING") {
            if (!projectConfig.securityConfig?.enabled) {
                setSecurityConfig({ enabled: true })
            }

            const generatedPermissions = tables.flatMap(t => [
                `${t.className.toUpperCase()}_READ`,
                `${t.className.toUpperCase()}_WRITE`,
                `${t.className.toUpperCase()}_DELETE`,
            ])

            const currentPerms = new Set(security.permissions || [])
            let hasNew = false
            generatedPermissions.forEach(p => {
                if (!currentPerms.has(p)) {
                    currentPerms.add(p)
                    hasNew = true
                }
            })

            let newRoles = [...(security.definedRoles || [])]
            if (newRoles.length === 0) {
                newRoles = [
                    { name: "USER", permissions: [] },
                    { name: "ADMIN", permissions: Array.from(currentPerms) },
                ]
                hasNew = true
            }

            if (hasNew) {
                setSecurityConfig({
                    permissions: Array.from(currentPerms).sort(),
                    definedRoles: newRoles,
                })
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectConfig.stackType, tables.length])

    // ─── Helpers ───────────────────────────────────────────────────────────────

    const toggleRolePermission = (roleName: string, permission: string) => {
        const updatedRoles = definedRoles.map(r => {
            if (r.name === roleName) {
                const hasPerm = r.permissions.includes(permission)
                return {
                    ...r,
                    permissions: hasPerm
                        ? r.permissions.filter(p => p !== permission)
                        : [...r.permissions, permission],
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

    const removeRole = (roleName: string) => {
        if (roleName === "ADMIN" || roleName === "USER") return
        setSecurityConfig({ definedRoles: definedRoles.filter(r => r.name !== roleName) })
    }

    const getRuleForEntity = (entityName: string, method: string) => {
        const path = `/api/${entityName.toLowerCase()}/**`
        return rules.find(r => r.path === path && r.method === method)
    }

    const handleEntityRuleChange = (entityName: string, method: string, level: string, role?: string) => {
        const path = `/api/${entityName.toLowerCase()}/**`
        const existingRule = rules.find(r => r.path === path && r.method === method)
        const newRule = {
            path,
            method,
            rule: level,
            role: role || (level === "HAS_ROLE" ? "ADMIN" : undefined),
        }
        if (existingRule) {
            removeSecurityRule(path, method)
            if (level !== "DEFAULT") addSecurityRule(newRule)
        } else {
            if (level !== "DEFAULT") addSecurityRule(newRule)
        }
    }

    const toggleSocialLogin = (provider: string) => {
        const current = security.socialLogins || []
        const isEnabled = current.includes(provider)
        const updated = isEnabled ? current.filter(p => p !== provider) : [...current, provider]
        setSecurityConfig({ socialLogins: updated })

        if (!isEnabled) {
            const configs = { ...(security.socialProviderConfigs || {}) }
            if (!configs[provider]) {
                configs[provider] = { clientId: "", clientSecret: "" }
            }
            setSecurityConfig({ socialProviderConfigs: configs })
        }
    }

    const updateSocialConfig = (provider: string, field: "clientId" | "clientSecret", value: string) => {
        const configs = { ...(security.socialProviderConfigs || {}) }
        configs[provider] = { ...(configs[provider] || { clientId: "", clientSecret: "" }), [field]: value }
        setSecurityConfig({ socialProviderConfigs: configs })
    }

    const filteredTables = tables.filter(t => t.name.toLowerCase().includes(searchTerm.toLowerCase()))

    const principalColumns = tables.find(t => t.className === security.principalEntity)?.columns || []

    // ─── Render ────────────────────────────────────────────────────────────────

    return (
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="max-w-5xl mx-auto">

                {/* ── Header ─────────────────────────────────────────────────── */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="text-center mb-10"
                >
                    <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-blue-500/20 mb-4 ring-1 ring-cyan-500/30">
                        <Shield className="w-8 h-8 text-cyan-400" />
                    </div>
                    <h1 className="text-3xl font-bold text-zinc-100 mb-2">Secure Your API</h1>
                    <p className="text-zinc-400 max-w-lg mx-auto">
                        Configure authentication strategy, identity mapping, roles, and endpoint-level access control.
                    </p>
                </motion.div>

                {/* ── Authentication Strategy Cards ──────────────────────────── */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.1 }}
                    className="mb-8"
                >
                    <Label className="text-sm font-semibold text-zinc-300 mb-3 block">
                        Authentication Strategy
                    </Label>
                    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
                        {AUTH_STRATEGIES.map(strategy => {
                            const Icon = strategy.icon
                            const isSelected = authType === strategy.value
                            return (
                                <button
                                    key={strategy.value}
                                    onClick={() => setSecurityConfig({ authenticationType: strategy.value })}
                                    className={`
                                        relative group flex flex-col items-center gap-2 p-4 rounded-xl border transition-all duration-200 text-center
                                        ${isSelected
                                            ? "bg-cyan-500/10 border-cyan-500/50 ring-1 ring-cyan-500/30 shadow-lg shadow-cyan-500/5"
                                            : "bg-zinc-900/60 border-zinc-700/50 hover:border-zinc-600 hover:bg-zinc-800/60"
                                        }
                                    `}
                                >
                                    <div className={`p-2.5 rounded-lg transition-colors ${isSelected ? "bg-cyan-500/20" : "bg-zinc-800"}`}>
                                        <Icon className={`w-5 h-5 ${isSelected ? "text-cyan-400" : "text-zinc-400"}`} />
                                    </div>
                                    <div>
                                        <p className={`text-sm font-semibold ${isSelected ? "text-cyan-300" : "text-zinc-200"}`}>
                                            {strategy.label}
                                        </p>
                                        <p className="text-[10px] text-zinc-500 mt-0.5 leading-tight">
                                            {strategy.description}
                                        </p>
                                    </div>
                                    {isSelected && (
                                        <motion.div
                                            layoutId="auth-indicator"
                                            className="absolute -top-1 -right-1 w-5 h-5 bg-cyan-500 rounded-full flex items-center justify-center"
                                        >
                                            <CheckCircle2 className="w-3.5 h-3.5 text-zinc-950" />
                                        </motion.div>
                                    )}
                                </button>
                            )
                        })}
                    </div>
                </motion.div>

                {/* ── Tabbed Main Content ─────────────────────────────────────── */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.2 }}
                >
                    <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
                        <TabsList className="w-full justify-start bg-zinc-900/80 border border-zinc-800 rounded-xl p-1 h-auto flex-wrap gap-1 mb-6">
                            {visibleTabs.map(tab => {
                                const Icon = tab.icon
                                return (
                                    <TabsTrigger
                                        key={tab.value}
                                        value={tab.value}
                                        className="rounded-lg px-3 py-2 text-xs font-medium data-[state=active]:bg-cyan-500/15 data-[state=active]:text-cyan-300 data-[state=active]:border-cyan-500/30 data-[state=active]:shadow-none text-zinc-400 border border-transparent hover:text-zinc-200 transition-all"
                                    >
                                        <Icon className="w-3.5 h-3.5 mr-1.5" />
                                        {tab.label}
                                    </TabsTrigger>
                                )
                            })}
                        </TabsList>

                        {/* ─── Identity Tab ──────────────────────────────────── */}
                            <TabsContent value="identity" className="mt-0">
                                <motion.div key="identity" {...tabMotion}>
                                    <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-6">
                                        <div className="flex items-center gap-3 mb-2">
                                            <div className="p-2 rounded-lg bg-blue-500/15">
                                                <User className="w-5 h-5 text-blue-400" />
                                            </div>
                                            <div>
                                                <h3 className="text-base font-semibold text-zinc-100">Identity & RBAC Model</h3>
                                                <p className="text-xs text-zinc-500">Map your user entity and define authorization architecture.</p>
                                            </div>
                                        </div>

                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                            {/* Principal Entity */}
                                            <div className="space-y-3">
                                                <Label className="text-zinc-300">Principal Entity (User Table)</Label>
                                                <Select
                                                    value={security.principalEntity}
                                                    onValueChange={value => setSecurityConfig({ principalEntity: value, usernameField: undefined, passwordField: undefined })}
                                                >
                                                    <SelectTrigger className="bg-zinc-950/50 border-zinc-700/50 text-zinc-200">
                                                        <SelectValue placeholder="Select User Entity" />
                                                    </SelectTrigger>
                                                    <SelectContent>
                                                        {tables.map(t => (
                                                            <SelectItem key={t.id} value={t.className}>{t.className}</SelectItem>
                                                        ))}
                                                    </SelectContent>
                                                </Select>

                                                {security.principalEntity && (
                                                    <motion.div
                                                        initial={{ opacity: 0, height: 0 }}
                                                        animate={{ opacity: 1, height: "auto" }}
                                                        className="grid grid-cols-2 gap-3"
                                                    >
                                                        <div className="space-y-1.5">
                                                            <Label className="text-xs text-zinc-500">Username Field</Label>
                                                            <Select
                                                                value={security.usernameField}
                                                                onValueChange={value => setSecurityConfig({ usernameField: value })}
                                                            >
                                                                <SelectTrigger className="h-9 text-xs bg-zinc-950/50 border-zinc-700/50 text-zinc-300">
                                                                    <SelectValue placeholder="e.g. email" />
                                                                </SelectTrigger>
                                                                <SelectContent>
                                                                    {principalColumns.map(c => (
                                                                        <SelectItem key={c.name} value={c.fieldName}>{c.fieldName}</SelectItem>
                                                                    ))}
                                                                </SelectContent>
                                                            </Select>
                                                        </div>
                                                        <div className="space-y-1.5">
                                                            <Label className="text-xs text-zinc-500">Password Field</Label>
                                                            <Select
                                                                value={security.passwordField}
                                                                onValueChange={value => setSecurityConfig({ passwordField: value })}
                                                            >
                                                                <SelectTrigger className="h-9 text-xs bg-zinc-950/50 border-zinc-700/50 text-zinc-300">
                                                                    <SelectValue placeholder="e.g. password" />
                                                                </SelectTrigger>
                                                                <SelectContent>
                                                                    {principalColumns.map(c => (
                                                                        <SelectItem key={c.name} value={c.fieldName}>{c.fieldName}</SelectItem>
                                                                    ))}
                                                                    <SelectItem value="password">password (Inject)</SelectItem>
                                                                </SelectContent>
                                                            </Select>
                                                        </div>
                                                    </motion.div>
                                                )}
                                            </div>

                                            {/* RBAC Mode */}
                                            <div className="space-y-3">
                                                <Label className="text-zinc-300">RBAC Mode</Label>
                                                <div className="flex bg-zinc-950/60 p-1 rounded-lg border border-zinc-800">
                                                    <button
                                                        onClick={() => setSecurityConfig({ rbacMode: "STATIC" })}
                                                        className={`flex-1 py-2.5 text-sm rounded-md transition-all font-medium ${
                                                            rbacMode === "STATIC"
                                                                ? "bg-cyan-500/20 text-cyan-300 shadow-sm border border-cyan-500/30"
                                                                : "text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800/60 border border-transparent"
                                                        }`}
                                                    >
                                                        Static (Enums)
                                                        <span className="block text-[10px] opacity-70 font-normal mt-0.5">Fast, Simple, No DB</span>
                                                    </button>
                                                    <button
                                                        onClick={() => setSecurityConfig({ rbacMode: "DYNAMIC" })}
                                                        className={`flex-1 py-2.5 text-sm rounded-md transition-all font-medium ${
                                                            rbacMode === "DYNAMIC"
                                                                ? "bg-cyan-500/20 text-cyan-300 shadow-sm border border-cyan-500/30"
                                                                : "text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800/60 border border-transparent"
                                                        }`}
                                                    >
                                                        Dynamic (Database)
                                                        <span className="block text-[10px] opacity-70 font-normal mt-0.5">Flexible, Admin Dashboard</span>
                                                    </button>
                                                </div>

                                                {rbacMode === "DYNAMIC" && (
                                                    <motion.div
                                                        initial={{ opacity: 0, height: 0 }}
                                                        animate={{ opacity: 1, height: "auto" }}
                                                    >
                                                        <Label className="text-xs text-zinc-500 mb-1.5 block">Role Entity (Optional Override)</Label>
                                                        <Select
                                                            value={security.roleEntity || "AUTO"}
                                                            onValueChange={value => setSecurityConfig({ roleEntity: value === "AUTO" ? undefined : value })}
                                                        >
                                                            <SelectTrigger className="bg-zinc-950/50 border-zinc-700/50 text-zinc-300">
                                                                <SelectValue placeholder="Auto-Generate Role Entity" />
                                                            </SelectTrigger>
                                                            <SelectContent>
                                                                <SelectItem value="AUTO">Auto-Generate (Recommended)</SelectItem>
                                                                {tables.filter(t => t.className !== security.principalEntity).map(t => (
                                                                    <SelectItem key={t.id} value={t.className}>{t.className}</SelectItem>
                                                                ))}
                                                            </SelectContent>
                                                        </Select>
                                                    </motion.div>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </motion.div>
                            </TabsContent>

                            {/* ─── Token & Session Tab (JWT only) ────────────── */}
                            {isJwt && (
                                <TabsContent value="token" className="mt-0">
                                    <motion.div key="token" {...tabMotion}>
                                        <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-6">
                                            <div className="flex items-center gap-3 mb-2">
                                                <div className="p-2 rounded-lg bg-amber-500/15">
                                                    <KeyRound className="w-5 h-5 text-amber-400" />
                                                </div>
                                                <div>
                                                    <h3 className="text-base font-semibold text-zinc-100">Token & Session</h3>
                                                    <p className="text-xs text-zinc-500">Configure JWT signing, refresh tokens, and session behavior.</p>
                                                </div>
                                            </div>

                                            {/* Signing Algorithm */}
                                            <div className="space-y-2">
                                                <Label className="text-zinc-300">JWT Signing Algorithm</Label>
                                                <div className="flex bg-zinc-950/60 p-1 rounded-lg border border-zinc-800 max-w-sm">
                                                    <button
                                                        onClick={() => setSecurityConfig({ signingAlgorithm: "HS256" })}
                                                        className={`flex-1 py-2 text-sm rounded-md font-medium transition-all ${
                                                            (security.signingAlgorithm || "HS256") === "HS256"
                                                                ? "bg-cyan-500/20 text-cyan-300 border border-cyan-500/30"
                                                                : "text-zinc-500 hover:text-zinc-300 border border-transparent"
                                                        }`}
                                                    >
                                                        HS256
                                                        <span className="block text-[10px] opacity-60 font-normal">Symmetric</span>
                                                    </button>
                                                    <button
                                                        onClick={() => setSecurityConfig({ signingAlgorithm: "RS256" })}
                                                        className={`flex-1 py-2 text-sm rounded-md font-medium transition-all ${
                                                            security.signingAlgorithm === "RS256"
                                                                ? "bg-cyan-500/20 text-cyan-300 border border-cyan-500/30"
                                                                : "text-zinc-500 hover:text-zinc-300 border border-transparent"
                                                        }`}
                                                    >
                                                        RS256
                                                        <span className="block text-[10px] opacity-60 font-normal">Asymmetric</span>
                                                    </button>
                                                </div>
                                            </div>

                                            {/* Toggle Options */}
                                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                                <ToggleRow
                                                    icon={<Database className="w-4 h-4 text-violet-400" />}
                                                    label="Refresh Token Persistence"
                                                    description={security.refreshTokenPersisted ? "Stored in database" : "In-memory only"}
                                                    checked={security.refreshTokenPersisted || false}
                                                    onCheckedChange={val => setSecurityConfig({ refreshTokenPersisted: val })}
                                                />
                                                <ToggleRow
                                                    icon={<RotateCcw className="w-4 h-4 text-emerald-400" />}
                                                    label="Remember Me"
                                                    description={security.rememberMeEnabled ? "Users stay signed in" : "Session expires normally"}
                                                    checked={security.rememberMeEnabled || false}
                                                    onCheckedChange={val => setSecurityConfig({ rememberMeEnabled: val })}
                                                />
                                                <ToggleRow
                                                    icon={<UserPlus className="w-4 h-4 text-blue-400" />}
                                                    label="Registration Endpoint"
                                                    description="Auto-generate /auth/register"
                                                    checked={security.registrationEnabled || false}
                                                    onCheckedChange={val => setSecurityConfig({ registrationEnabled: val })}
                                                />
                                            </div>

                                            {/* Remember Me Mode (only if rememberMe is enabled) */}
                                            {security.rememberMeEnabled && (
                                                <motion.div
                                                    initial={{ opacity: 0, height: 0 }}
                                                    animate={{ opacity: 1, height: "auto" }}
                                                    className="pl-4 border-l-2 border-emerald-500/30"
                                                >
                                                    <Label className="text-xs text-zinc-400 mb-2 block">Remember Me Mode</Label>
                                                    <div className="flex bg-zinc-950/60 p-1 rounded-lg border border-zinc-800 max-w-xs">
                                                        <button
                                                            onClick={() => setSecurityConfig({ rememberMeMode: "ALWAYS" })}
                                                            className={`flex-1 py-1.5 text-xs rounded-md font-medium transition-all ${
                                                                (security.rememberMeMode || "ALWAYS") === "ALWAYS"
                                                                    ? "bg-emerald-500/20 text-emerald-300 border border-emerald-500/30"
                                                                    : "text-zinc-500 border border-transparent"
                                                            }`}
                                                        >
                                                            Always
                                                        </button>
                                                        <button
                                                            onClick={() => setSecurityConfig({ rememberMeMode: "CHECKBOX" })}
                                                            className={`flex-1 py-1.5 text-xs rounded-md font-medium transition-all ${
                                                                security.rememberMeMode === "CHECKBOX"
                                                                    ? "bg-emerald-500/20 text-emerald-300 border border-emerald-500/30"
                                                                    : "text-zinc-500 border border-transparent"
                                                            }`}
                                                        >
                                                            Checkbox
                                                        </button>
                                                    </div>
                                                </motion.div>
                                            )}

                                            {/* Refresh Token Entity (only if persisted) */}
                                            {security.refreshTokenPersisted && (
                                                <motion.div
                                                    initial={{ opacity: 0, height: 0 }}
                                                    animate={{ opacity: 1, height: "auto" }}
                                                    className="pl-4 border-l-2 border-violet-500/30"
                                                >
                                                    <Label className="text-xs text-zinc-400 mb-2 block">Refresh Token Entity</Label>
                                                    <Select
                                                        value={security.refreshTokenEntity || "AUTO"}
                                                        onValueChange={value => setSecurityConfig({ refreshTokenEntity: value === "AUTO" ? undefined : value })}
                                                    >
                                                        <SelectTrigger className="bg-zinc-950/50 border-zinc-700/50 text-zinc-300 max-w-xs">
                                                            <SelectValue placeholder="Auto-generate entity" />
                                                        </SelectTrigger>
                                                        <SelectContent>
                                                            <SelectItem value="AUTO">Auto-Generate</SelectItem>
                                                            {tables.map(t => (
                                                                <SelectItem key={t.id} value={t.className}>{t.className}</SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>
                                                </motion.div>
                                            )}
                                        </div>
                                    </motion.div>
                                </TabsContent>
                            )}

                            {/* ─── Social Logins Tab (JWT only) ──────────────── */}
                            {isJwt && (
                                <TabsContent value="social" className="mt-0">
                                    <motion.div key="social" {...tabMotion}>
                                        <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-6">
                                            <div className="flex items-center gap-3 mb-2">
                                                <div className="p-2 rounded-lg bg-indigo-500/15">
                                                    <Globe className="w-5 h-5 text-indigo-400" />
                                                </div>
                                                <div>
                                                    <h3 className="text-base font-semibold text-zinc-100">Social Logins</h3>
                                                    <p className="text-xs text-zinc-500">Enable OAuth2 social identity providers.</p>
                                                </div>
                                            </div>

                                            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                                                {SOCIAL_PROVIDERS.map(provider => {
                                                    const Icon = provider.icon
                                                    const isEnabled = (security.socialLogins || []).includes(provider.id)
                                                    const config = security.socialProviderConfigs?.[provider.id]

                                                    return (
                                                        <div
                                                            key={provider.id}
                                                            className={`rounded-xl border transition-all ${
                                                                isEnabled
                                                                    ? "bg-zinc-800/60 border-cyan-500/30"
                                                                    : "bg-zinc-900/40 border-zinc-800 hover:border-zinc-700"
                                                            }`}
                                                        >
                                                            <div className="p-4">
                                                                <div className="flex items-center justify-between mb-3">
                                                                    <div className="flex items-center gap-2">
                                                                        <Icon className={`w-5 h-5 ${provider.color}`} />
                                                                        <span className="text-sm font-medium text-zinc-200">{provider.label}</span>
                                                                    </div>
                                                                    <Switch
                                                                        checked={isEnabled}
                                                                        onCheckedChange={() => toggleSocialLogin(provider.id)}
                                                                    />
                                                                </div>

                                                                {isEnabled && (
                                                                    <motion.div
                                                                        initial={{ opacity: 0, height: 0 }}
                                                                        animate={{ opacity: 1, height: "auto" }}
                                                                        className="space-y-2 pt-2 border-t border-zinc-700/50"
                                                                    >
                                                                        <div className="space-y-1">
                                                                            <Label className="text-[10px] text-zinc-500 uppercase tracking-wider">Client ID</Label>
                                                                            <Input
                                                                                value={config?.clientId || ""}
                                                                                onChange={e => updateSocialConfig(provider.id, "clientId", e.target.value)}
                                                                                placeholder="your-client-id"
                                                                                className="h-8 text-xs bg-zinc-950/50 border-zinc-700/50 text-zinc-300"
                                                                            />
                                                                        </div>
                                                                        <div className="space-y-1">
                                                                            <Label className="text-[10px] text-zinc-500 uppercase tracking-wider">Client Secret</Label>
                                                                            <Input
                                                                                type="password"
                                                                                value={config?.clientSecret || ""}
                                                                                onChange={e => updateSocialConfig(provider.id, "clientSecret", e.target.value)}
                                                                                placeholder="your-client-secret"
                                                                                className="h-8 text-xs bg-zinc-950/50 border-zinc-700/50 text-zinc-300"
                                                                            />
                                                                        </div>
                                                                    </motion.div>
                                                                )}
                                                            </div>
                                                        </div>
                                                    )
                                                })}
                                            </div>
                                        </div>
                                    </motion.div>
                                </TabsContent>
                            )}

                            {/* ─── Keycloak Tab ──────────────────────────────── */}
                            {isKeycloak && (
                                <TabsContent value="keycloak" className="mt-0">
                                    <motion.div key="keycloak" {...tabMotion}>
                                        <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-6">
                                            <div className="flex items-center gap-3 mb-2">
                                                <div className="p-2 rounded-lg bg-orange-500/15">
                                                    <ServerCog className="w-5 h-5 text-orange-400" />
                                                </div>
                                                <div>
                                                    <h3 className="text-base font-semibold text-zinc-100">Keycloak Configuration</h3>
                                                    <p className="text-xs text-zinc-500">Connect your Spring Boot app to a Keycloak identity server.</p>
                                                </div>
                                            </div>

                                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                                <div className="space-y-1.5">
                                                    <Label className="text-zinc-400 text-sm">Realm Name</Label>
                                                    <Input
                                                        value={security.keycloakRealm || ""}
                                                        onChange={e => setSecurityConfig({ keycloakRealm: e.target.value })}
                                                        placeholder="my-realm"
                                                        className="bg-zinc-950/50 border-zinc-700/50 text-zinc-200"
                                                    />
                                                </div>
                                                <div className="space-y-1.5">
                                                    <Label className="text-zinc-400 text-sm">Client ID</Label>
                                                    <Input
                                                        value={security.keycloakClientId || ""}
                                                        onChange={e => setSecurityConfig({ keycloakClientId: e.target.value })}
                                                        placeholder="spring-app"
                                                        className="bg-zinc-950/50 border-zinc-700/50 text-zinc-200"
                                                    />
                                                </div>
                                                <div className="space-y-1.5">
                                                    <Label className="text-zinc-400 text-sm">Client Secret</Label>
                                                    <Input
                                                        type="password"
                                                        value={security.keycloakClientSecret || ""}
                                                        onChange={e => setSecurityConfig({ keycloakClientSecret: e.target.value })}
                                                        placeholder="secret"
                                                        className="bg-zinc-950/50 border-zinc-700/50 text-zinc-200"
                                                    />
                                                </div>
                                                <div className="space-y-1.5">
                                                    <Label className="text-zinc-400 text-sm">Issuer URL</Label>
                                                    <Input
                                                        value={security.keycloakIssuerUrl || ""}
                                                        onChange={e => setSecurityConfig({ keycloakIssuerUrl: e.target.value })}
                                                        placeholder="https://keycloak.example.com/realms/my-realm"
                                                        className="bg-zinc-950/50 border-zinc-700/50 text-zinc-200"
                                                    />
                                                </div>
                                            </div>
                                        </div>
                                    </motion.div>
                                </TabsContent>
                            )}

                            {/* ─── Roles & Permissions Tab ───────────────────── */}
                            <TabsContent value="roles" className="mt-0">
                                <motion.div key="roles" {...tabMotion}>
                                    <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-6">
                                        <div className="flex items-center gap-3 mb-2">
                                            <div className="p-2 rounded-lg bg-purple-500/15">
                                                <Users className="w-5 h-5 text-purple-400" />
                                            </div>
                                            <div>
                                                <h3 className="text-base font-semibold text-zinc-100">Roles & Permissions</h3>
                                                <p className="text-xs text-zinc-500">Define roles and assign auto-generated or custom permissions.</p>
                                            </div>
                                        </div>

                                        <Tabs defaultValue="roles-editor" className="w-full">
                                            <TabsList className="w-full justify-start bg-transparent border-b border-zinc-800 rounded-none h-9 p-0 mb-4">
                                                <TabsTrigger
                                                    value="roles-editor"
                                                    className="rounded-none border-b-2 border-transparent data-[state=active]:border-cyan-400 data-[state=active]:bg-transparent data-[state=active]:text-cyan-300 data-[state=active]:shadow-none px-4 text-xs text-zinc-400"
                                                >
                                                    Roles
                                                </TabsTrigger>
                                                <TabsTrigger
                                                    value="permissions-list"
                                                    className="rounded-none border-b-2 border-transparent data-[state=active]:border-cyan-400 data-[state=active]:bg-transparent data-[state=active]:text-cyan-300 data-[state=active]:shadow-none px-4 text-xs text-zinc-400"
                                                >
                                                    Permissions ({availablePermissions.length})
                                                </TabsTrigger>
                                            </TabsList>

                                            <TabsContent value="roles-editor" className="space-y-4">
                                                <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                                                    {definedRoles.map(role => (
                                                        <div key={role.name} className="bg-zinc-950/40 p-4 rounded-xl border border-zinc-800">
                                                            <div className="flex items-center justify-between mb-3">
                                                                <div className="flex items-center gap-2">
                                                                    <Badge variant="outline" className="font-mono text-cyan-300 border-cyan-500/30">
                                                                        {role.name}
                                                                    </Badge>
                                                                    <span className="text-[10px] text-zinc-500">
                                                                        {role.permissions.length} permissions
                                                                    </span>
                                                                </div>
                                                                {role.name !== "ADMIN" && role.name !== "USER" && (
                                                                    <Tooltip>
                                                                        <TooltipTrigger asChild>
                                                                            <button
                                                                                onClick={() => removeRole(role.name)}
                                                                                className="text-zinc-600 hover:text-red-400 transition-colors"
                                                                            >
                                                                                <Circle className="w-3 h-3" />
                                                                            </button>
                                                                        </TooltipTrigger>
                                                                        <TooltipContent>Remove role</TooltipContent>
                                                                    </Tooltip>
                                                                )}
                                                            </div>
                                                            <div className="h-44 overflow-y-auto pr-2 space-y-0.5 custom-scrollbar">
                                                                {availablePermissions.map(perm => {
                                                                    const hasPerm = role.permissions.includes(perm)
                                                                    return (
                                                                        <label
                                                                            key={perm}
                                                                            className="flex items-center gap-2 text-xs py-1.5 px-2 hover:bg-zinc-800/60 rounded cursor-pointer transition-colors"
                                                                        >
                                                                            <input
                                                                                type="checkbox"
                                                                                checked={hasPerm}
                                                                                onChange={() => toggleRolePermission(role.name, perm)}
                                                                                className="rounded border-zinc-600 bg-zinc-950 h-3 w-3 text-cyan-500 focus:ring-cyan-500/30"
                                                                            />
                                                                            <span className={hasPerm ? "text-cyan-300" : "text-zinc-500"}>
                                                                                {perm}
                                                                            </span>
                                                                        </label>
                                                                    )
                                                                })}
                                                            </div>
                                                        </div>
                                                    ))}

                                                    <button
                                                        onClick={addRole}
                                                        className="h-full min-h-[200px] border-2 border-dashed border-zinc-800 rounded-xl flex flex-col items-center justify-center text-zinc-500 hover:text-cyan-400 hover:border-cyan-500/30 hover:bg-cyan-500/5 transition-all gap-2"
                                                    >
                                                        <Plus className="w-6 h-6" />
                                                        <span className="text-sm">Add Role</span>
                                                    </button>
                                                </div>
                                            </TabsContent>

                                            <TabsContent value="permissions-list" className="space-y-4">
                                                <div className="bg-zinc-950/40 p-4 rounded-xl border border-zinc-800">
                                                    <div className="flex flex-wrap gap-2">
                                                        {availablePermissions.map(perm => (
                                                            <Badge
                                                                key={perm}
                                                                variant="secondary"
                                                                className="font-mono text-zinc-300 bg-zinc-800 border border-zinc-700/50"
                                                            >
                                                                {perm}
                                                            </Badge>
                                                        ))}
                                                        <Button
                                                            variant="outline"
                                                            size="sm"
                                                            className="h-6 text-xs border-dashed border-zinc-700 text-zinc-400 hover:text-cyan-300 hover:border-cyan-500/30"
                                                            onClick={() => {
                                                                const p = prompt("Enter Custom Permission (e.g. REPORT_EXPORT):")
                                                                if (p) {
                                                                    const clean = p.toUpperCase().replace(/[^A-Z0-9_]/g, "_")
                                                                    if (!availablePermissions.includes(clean)) {
                                                                        setSecurityConfig({ permissions: [...availablePermissions, clean].sort() })
                                                                    }
                                                                }
                                                            }}
                                                        >
                                                            <Plus className="w-3 h-3 mr-1" /> Add
                                                        </Button>
                                                    </div>
                                                </div>
                                            </TabsContent>
                                        </Tabs>
                                    </div>
                                </motion.div>
                            </TabsContent>

                            {/* ─── Endpoints Tab ─────────────────────────────── */}
                            <TabsContent value="endpoints" className="mt-0">
                                <motion.div key="endpoints" {...tabMotion}>
                                    <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-5">
                                        <div className="flex items-center gap-3 mb-2">
                                            <div className="p-2 rounded-lg bg-emerald-500/15">
                                                <Database className="w-5 h-5 text-emerald-400" />
                                            </div>
                                            <div>
                                                <h3 className="text-base font-semibold text-zinc-100">Endpoint Permissions</h3>
                                                <p className="text-xs text-zinc-500">Set access levels per HTTP method for each resource entity.</p>
                                            </div>
                                        </div>

                                        {/* Search */}
                                        <div className="relative">
                                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4" />
                                            <Input
                                                placeholder="Search entities..."
                                                className="pl-10 h-10 bg-zinc-950/50 border-zinc-700/50 text-zinc-300 placeholder:text-zinc-600"
                                                value={searchTerm}
                                                onChange={e => setSearchTerm(e.target.value)}
                                            />
                                        </div>

                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                            {filteredTables.map(table => (
                                                <motion.div
                                                    key={table.id}
                                                    initial={{ opacity: 0, scale: 0.97 }}
                                                    animate={{ opacity: 1, scale: 1 }}
                                                    className="bg-zinc-950/40 rounded-xl overflow-hidden border border-zinc-800 hover:border-zinc-700 transition-all"
                                                >
                                                    <div className="px-4 py-3 bg-zinc-800/40 border-b border-zinc-800 flex items-center justify-between">
                                                        <h4 className="text-sm font-bold text-zinc-200 flex items-center gap-2">
                                                            <div className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                                                            {table.className}
                                                        </h4>
                                                        <Badge variant="outline" className="text-[10px] text-zinc-500 border-zinc-700">
                                                            /api/{table.className.toLowerCase()}
                                                        </Badge>
                                                    </div>

                                                    <div className="p-4 space-y-2.5">
                                                        {["GET", "POST", "PUT", "DELETE"].map(method => {
                                                            const rule = getRuleForEntity(table.className, method)
                                                            const currentLevel = rule?.rule || "AUTHENTICATED"
                                                            const methodColors: Record<string, string> = {
                                                                GET: "text-emerald-400 bg-emerald-500/10",
                                                                POST: "text-blue-400 bg-blue-500/10",
                                                                PUT: "text-amber-400 bg-amber-500/10",
                                                                DELETE: "text-red-400 bg-red-500/10",
                                                            }

                                                            return (
                                                                <div key={method} className="flex items-center justify-between group">
                                                                    <Badge variant="secondary" className={`w-16 justify-center font-mono text-[10px] ${methodColors[method]}`}>
                                                                        {method}
                                                                    </Badge>

                                                                    <div className="flex bg-zinc-950/60 rounded-lg p-0.5 gap-0.5 flex-wrap">
                                                                        <button
                                                                            onClick={() => handleEntityRuleChange(table.className, method, "PERMIT_ALL")}
                                                                            className={`px-2.5 py-1 text-[10px] rounded-md font-medium transition-all ${
                                                                                currentLevel === "PERMIT_ALL"
                                                                                    ? "bg-emerald-500 text-white shadow-lg shadow-emerald-500/20"
                                                                                    : "text-zinc-500 hover:bg-zinc-800"
                                                                            }`}
                                                                        >
                                                                            Public
                                                                        </button>
                                                                        <button
                                                                            onClick={() => handleEntityRuleChange(table.className, method, "AUTHENTICATED")}
                                                                            className={`px-2.5 py-1 text-[10px] rounded-md font-medium transition-all ${
                                                                                currentLevel === "AUTHENTICATED"
                                                                                    ? "bg-blue-500 text-white shadow-lg shadow-blue-500/20"
                                                                                    : "text-zinc-500 hover:bg-zinc-800"
                                                                            }`}
                                                                        >
                                                                            Auth
                                                                        </button>
                                                                        {definedRoles.map((role, idx) => {
                                                                            const isActive = currentLevel === "HAS_ROLE" && rule?.role === role.name
                                                                            const roleColors = [
                                                                                "bg-red-500 shadow-red-500/20",
                                                                                "bg-purple-500 shadow-purple-500/20",
                                                                                "bg-orange-500 shadow-orange-500/20",
                                                                                "bg-pink-500 shadow-pink-500/20",
                                                                            ]
                                                                            return (
                                                                                <button
                                                                                    key={role.name}
                                                                                    onClick={() => handleEntityRuleChange(table.className, method, "HAS_ROLE", role.name)}
                                                                                    className={`px-2.5 py-1 text-[10px] rounded-md font-medium transition-all ${
                                                                                        isActive
                                                                                            ? `${roleColors[idx % roleColors.length]} text-white shadow-lg`
                                                                                            : "text-zinc-500 hover:bg-zinc-800"
                                                                                    }`}
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

                                        {filteredTables.length === 0 && (
                                            <div className="text-center py-12 text-zinc-600">
                                                <Database className="w-8 h-8 mx-auto mb-2 opacity-40" />
                                                <p className="text-sm">No entities found. Define tables in the Schema phase first.</p>
                                            </div>
                                        )}
                                    </div>
                                </motion.div>
                            </TabsContent>

                            {/* ─── Advanced Tab ──────────────────────────────── */}
                            <TabsContent value="advanced" className="mt-0">
                                <motion.div key="advanced" {...tabMotion}>
                                    <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-6">
                                        <div className="flex items-center gap-3 mb-2">
                                            <div className="p-2 rounded-lg bg-zinc-500/15">
                                                <Settings className="w-5 h-5 text-zinc-400" />
                                            </div>
                                            <div>
                                                <h3 className="text-base font-semibold text-zinc-100">Advanced Settings</h3>
                                                <p className="text-xs text-zinc-500">Password reset, code style, fallback, and testing options.</p>
                                            </div>
                                        </div>

                                        {/* Password Reset */}
                                        <div className="space-y-3">
                                            <ToggleRow
                                                icon={<FileKey className="w-4 h-4 text-rose-400" />}
                                                label="Password Reset"
                                                description="Generate password reset flow with token"
                                                checked={security.passwordResetEnabled || false}
                                                onCheckedChange={val => setSecurityConfig({ passwordResetEnabled: val })}
                                            />

                                            {security.passwordResetEnabled && security.principalEntity && (
                                                <motion.div
                                                    initial={{ opacity: 0, height: 0 }}
                                                    animate={{ opacity: 1, height: "auto" }}
                                                    className="pl-4 border-l-2 border-rose-500/30 grid grid-cols-2 gap-3"
                                                >
                                                    <div className="space-y-1.5">
                                                        <Label className="text-xs text-zinc-500">Token Field</Label>
                                                        <Select
                                                            value={security.passwordResetTokenField || ""}
                                                            onValueChange={val => setSecurityConfig({ passwordResetTokenField: val })}
                                                        >
                                                            <SelectTrigger className="h-9 text-xs bg-zinc-950/50 border-zinc-700/50 text-zinc-300">
                                                                <SelectValue placeholder="Select field" />
                                                            </SelectTrigger>
                                                            <SelectContent>
                                                                {principalColumns.map(c => (
                                                                    <SelectItem key={c.name} value={c.fieldName}>{c.fieldName}</SelectItem>
                                                                ))}
                                                                <SelectItem value="resetToken">resetToken (Inject)</SelectItem>
                                                            </SelectContent>
                                                        </Select>
                                                    </div>
                                                    <div className="space-y-1.5">
                                                        <Label className="text-xs text-zinc-500">Expiry Field</Label>
                                                        <Select
                                                            value={security.passwordResetExpiryField || ""}
                                                            onValueChange={val => setSecurityConfig({ passwordResetExpiryField: val })}
                                                        >
                                                            <SelectTrigger className="h-9 text-xs bg-zinc-950/50 border-zinc-700/50 text-zinc-300">
                                                                <SelectValue placeholder="Select field" />
                                                            </SelectTrigger>
                                                            <SelectContent>
                                                                {principalColumns.map(c => (
                                                                    <SelectItem key={c.name} value={c.fieldName}>{c.fieldName}</SelectItem>
                                                                ))}
                                                                <SelectItem value="resetExpiry">resetExpiry (Inject)</SelectItem>
                                                            </SelectContent>
                                                        </Select>
                                                    </div>
                                                </motion.div>
                                            )}
                                        </div>

                                        <div className="border-t border-zinc-800" />

                                        {/* Security Style */}
                                        <div className="space-y-2">
                                            <Label className="text-zinc-300 text-sm flex items-center gap-2">
                                                <Layers className="w-4 h-4 text-zinc-400" />
                                                Security Code Style
                                            </Label>
                                            <div className="flex bg-zinc-950/60 p-1 rounded-lg border border-zinc-800 max-w-sm">
                                                <button
                                                    onClick={() => setSecurityConfig({ securityStyle: "ANNOTATION" })}
                                                    className={`flex-1 py-2 text-sm rounded-md font-medium transition-all ${
                                                        (security.securityStyle || "ANNOTATION") === "ANNOTATION"
                                                            ? "bg-cyan-500/20 text-cyan-300 border border-cyan-500/30"
                                                            : "text-zinc-500 hover:text-zinc-300 border border-transparent"
                                                    }`}
                                                >
                                                    Annotation
                                                    <span className="block text-[10px] opacity-60 font-normal">@PreAuthorize</span>
                                                </button>
                                                <button
                                                    onClick={() => setSecurityConfig({ securityStyle: "CONFIG" })}
                                                    className={`flex-1 py-2 text-sm rounded-md font-medium transition-all ${
                                                        security.securityStyle === "CONFIG"
                                                            ? "bg-cyan-500/20 text-cyan-300 border border-cyan-500/30"
                                                            : "text-zinc-500 hover:text-zinc-300 border border-transparent"
                                                    }`}
                                                >
                                                    Config
                                                    <span className="block text-[10px] opacity-60 font-normal">SecurityFilterChain</span>
                                                </button>
                                            </div>
                                        </div>

                                        <div className="border-t border-zinc-800" />

                                        {/* Fallback & Testing Toggles */}
                                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                            <ToggleRow
                                                icon={<ToggleLeft className="w-4 h-4 text-yellow-400" />}
                                                label="Static User Fallback"
                                                description="In-memory users when no user table"
                                                checked={security.staticUserFallback || false}
                                                onCheckedChange={val => setSecurityConfig({ staticUserFallback: val })}
                                            />
                                            <ToggleRow
                                                icon={<FlaskConical className="w-4 h-4 text-teal-400" />}
                                                label="Test Users"
                                                description="Seed test users in dev profile"
                                                checked={security.testUsersEnabled || false}
                                                onCheckedChange={val => setSecurityConfig({ testUsersEnabled: val })}
                                            />
                                        </div>
                                    </div>
                                </motion.div>
                            </TabsContent>
                    </Tabs>
                </motion.div>

                {/* ── Navigation ──────────────────────────────────────────────── */}
                <div className="flex justify-between mt-8">
                    <Button
                        variant="ghost"
                        className="text-zinc-400 hover:text-zinc-200"
                        onClick={() => useGeneratorStore.getState().setCurrentPhase(2)}
                    >
                        Back
                    </Button>
                    <Button
                        onClick={() => useGeneratorStore.getState().setCurrentPhase(4)}
                        className="bg-gradient-to-r from-cyan-600 to-blue-600 text-white hover:opacity-90"
                    >
                        Continue to Project Settings
                        <ArrowRight className="w-4 h-4 ml-1" />
                    </Button>
                </div>
            </div>
        </div>
    )
}

// ─── Sub-components ─────────────────────────────────────────────────────────────

function ToggleRow({
    icon,
    label,
    description,
    checked,
    onCheckedChange,
}: {
    icon: React.ReactNode
    label: string
    description: string
    checked: boolean
    onCheckedChange: (val: boolean) => void
}) {
    return (
        <div className="flex items-center justify-between gap-4 bg-zinc-950/40 border border-zinc-800 rounded-xl px-4 py-3">
            <div className="flex items-center gap-3 min-w-0">
                <div className="shrink-0">{icon}</div>
                <div className="min-w-0">
                    <p className="text-sm font-medium text-zinc-200 truncate">{label}</p>
                    <p className="text-[11px] text-zinc-500 truncate">{description}</p>
                </div>
            </div>
            <Switch checked={checked} onCheckedChange={onCheckedChange} />
        </div>
    )
}
