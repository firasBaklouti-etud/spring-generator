"use client"

import type React from "react"

import { motion } from "framer-motion"
import { Check } from "lucide-react"
import { useGeneratorStore, AVAILABLE_STACKS, type StackType } from "@/lib/store"
import { cn } from "@/lib/utils"

const stackIcons: Record<StackType, React.ReactNode> = {
    SPRING: (
        <svg viewBox="0 0 24 24" className="w-6 h-6" fill="currentColor">
            <path d="M21.8 2.2c-.1-.1-.2-.2-.4-.2-.1 0-.3 0-.4.1-3.1 1.5-5.3 3.5-6.7 6.1-.3.6-.6 1.2-.8 1.8-.2-.2-.5-.4-.7-.6-2.5-2.1-6.2-1.8-8.3.7-1.1 1.3-1.5 2.9-1.3 4.5.3 1.6 1.2 3 2.5 3.9 1 .7 2.2 1.1 3.4 1.1.4 0 .9 0 1.3-.1 1.6-.3 3-1.2 3.9-2.5.1-.1.1-.2.2-.3.2.1.4.2.6.3 1.8.7 3.8.3 5.2-1 1.4-1.4 1.8-3.4 1.1-5.2l-.1-.2c.1-.1.2-.2.3-.4 1.3-1.9 1.7-4.2 1.2-6.4-.1-.5-.4-.9-.7-1.1-.3-.1-.6-.2-.9-.1-.9.2-1.5.9-1.7 1.8-.1.6-.2 1.2-.2 1.8 0 .4 0 .8.1 1.2l.1.4c-.3.5-.7 1-1.1 1.4-.3-.1-.6-.2-.9-.2-.5-.1-1-.1-1.5 0 .3-.7.6-1.4 1-2 1.1-1.8 2.6-3.4 4.5-4.5.5-.3.8-.8.8-1.4 0-.4-.2-.7-.4-.9z" />
        </svg>
    ),
    NODE: (
        <svg viewBox="0 0 24 24" className="w-6 h-6" fill="currentColor">
            <path d="M12 1.85c-.27 0-.55.07-.78.2L3.78 6.35c-.48.28-.78.8-.78 1.36v8.58c0 .56.3 1.08.78 1.36l7.44 4.3c.23.13.5.2.78.2s.55-.07.78-.2l7.44-4.3c.48-.28.78-.8.78-1.36V7.71c0-.56-.3-1.08-.78-1.36l-7.44-4.3c-.23-.13-.51-.2-.78-.2z" />
        </svg>
    ),
    NEST: (
        <svg viewBox="0 0 24 24" className="w-6 h-6" fill="currentColor">
            <path d="M14.131.047c-.173 0-.334.037-.483.087.316.21.49.49.576.806.007.043.019.074.025.117a.681.681 0 0 1 .013.112c.024.545-.143.614-.218.936-.034.142-.048.347.015.553.074.241.249.425.461.581a.89.89 0 0 0 .322-.153c.105-.082.196-.192.258-.32.096-.194.122-.435.098-.65-.035-.317-.147-.617-.281-.896-.058-.124-.125-.242-.178-.37-.104-.249-.171-.498-.171-.754 0-.257.085-.502.237-.702.072-.096.15-.181.236-.254a2.029 2.029 0 0 0-.91-.093zM16.102.4c-.024.143-.006.242.009.387.015.145.037.297.044.46.006.163-.012.346-.087.509-.075.163-.2.304-.366.39a.882.882 0 0 1-.466.103c-.093-.008-.19-.032-.29-.078-.108-.05-.233-.135-.29-.166.009.18.098.35.217.484.118.135.27.233.448.281a.848.848 0 0 0 .524-.035.779.779 0 0 0 .376-.285c.093-.128.15-.282.177-.44a.985.985 0 0 0-.026-.483c-.046-.151-.124-.29-.22-.417a1.01 1.01 0 0 1-.05-.71zM11.094.646c-.106.074-.178.151-.254.234-.257.285-.447.613-.563.966-.116.354-.146.724-.065 1.076.057.25.174.481.323.684.15.203.33.378.52.54-.091-.42-.071-.866.092-1.271.082-.203.2-.388.341-.548.14-.16.308-.294.484-.415a2.88 2.88 0 0 1-.274-.458 1.78 1.78 0 0 1-.171-.579c-.025-.21.007-.425.098-.618-.241.088-.422.236-.531.39zM9.274 1.99c-.073.205-.11.438-.078.667.033.229.135.446.29.613.155.168.36.293.574.369.214.076.442.103.671.092-.215-.134-.39-.293-.516-.476-.126-.183-.199-.393-.226-.608a1.04 1.04 0 0 1 .076-.498c.058-.145.148-.28.262-.385a1.13 1.13 0 0 0-.483.021c-.22.046-.42.127-.57.205zM12 2.125c-.36 0-.691.094-.972.254a1.87 1.87 0 0 0-.691.706c-.168.306-.263.655-.281 1.012-.018.357.04.72.17 1.057.26.675.755 1.248 1.372 1.585.617.337 1.357.439 2.042.295a2.414 2.414 0 0 0 1.016-.458 2.393 2.393 0 0 0 .692-.788c.165-.306.259-.644.28-.99.02-.346-.037-.696-.164-1.02-.255-.65-.74-1.2-1.345-1.527a2.453 2.453 0 0 0-1.119-.126z" />
        </svg>
    ),
    FASTAPI: (
        <svg viewBox="0 0 24 24" className="w-6 h-6" fill="currentColor">
            <path d="M12 0C5.375 0 0 5.375 0 12c0 6.627 5.375 12 12 12 6.626 0 12-5.373 12-12 0-6.625-5.373-12-12-12zm-.624 21.62v-7.528H7.19L13.203 2.38v7.528h4.029L11.376 21.62z" />
        </svg>
    ),
}

const stackColors: Record<StackType, { bg: string; border: string; text: string; glow: string }> = {
    SPRING: {
        bg: "bg-green-500/10",
        border: "border-green-500/30",
        text: "text-green-400",
        glow: "shadow-green-500/20",
    },
    NODE: {
        bg: "bg-emerald-500/10",
        border: "border-emerald-500/30",
        text: "text-emerald-400",
        glow: "shadow-emerald-500/20",
    },
    NEST: {
        bg: "bg-red-500/10",
        border: "border-red-500/30",
        text: "text-red-400",
        glow: "shadow-red-500/20",
    },
    FASTAPI: {
        bg: "bg-teal-500/10",
        border: "border-teal-500/30",
        text: "text-teal-400",
        glow: "shadow-teal-500/20",
    },
}

interface StackSelectorProps {
    className?: string
}

export function StackSelector({ className }: StackSelectorProps) {
    const { projectConfig, setProjectConfig } = useGeneratorStore()
    const selectedStack = projectConfig.stackType

    const handleStackChange = (stackId: StackType) => {
        setProjectConfig({ stackType: stackId })
    }

    return (
        <div className={cn("space-y-3", className)}>
            <label className="text-sm font-medium text-foreground">Technology Stack</label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                {AVAILABLE_STACKS.map((stack) => {
                    const isSelected = selectedStack === stack.id
                    const colors = stackColors[stack.id]

                    return (
                        <motion.button
                            key={stack.id}
                            onClick={() => handleStackChange(stack.id)}
                            whileHover={{ scale: 1.02 }}
                            whileTap={{ scale: 0.98 }}
                            className={cn(
                                "relative flex flex-col items-center gap-2 p-4 rounded-xl border-2 transition-all duration-200",
                                isSelected
                                    ? cn(colors.bg, colors.border, "shadow-lg", colors.glow)
                                    : "bg-secondary/20 border-border hover:border-primary/30 hover:bg-secondary/40",
                            )}
                        >
                            {/* Selection indicator */}
                            {isSelected && (
                                <motion.div
                                    initial={{ scale: 0 }}
                                    animate={{ scale: 1 }}
                                    className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-primary flex items-center justify-center"
                                >
                                    <Check className="w-3 h-3 text-primary-foreground" />
                                </motion.div>
                            )}

                            {/* Stack icon */}
                            <div className={cn("transition-colors", isSelected ? colors.text : "text-muted-foreground")}>
                                {stackIcons[stack.id]}
                            </div>

                            {/* Stack name */}
                            <span
                                className={cn(
                                    "text-sm font-medium transition-colors",
                                    isSelected ? "text-foreground" : "text-muted-foreground",
                                )}
                            >
                                {stack.displayName}
                            </span>

                            {/* Language badge */}
                            <span
                                className={cn(
                                    "text-[10px] uppercase tracking-wider px-2 py-0.5 rounded-full",
                                    isSelected ? cn(colors.bg, colors.text) : "bg-muted/50 text-muted-foreground",
                                )}
                            >
                                {stack.language}
                            </span>
                        </motion.button>
                    )
                })}
            </div>
        </div>
    )
}
