"use client"

import { motion } from "framer-motion"
import { FileCode2, Workflow, Rocket, Check } from "lucide-react"
import { useGeneratorStore } from "@/lib/store"

const phases = [
  { id: 1, label: "SQL Parser", icon: FileCode2 },
  { id: 2, label: "Schema Editor", icon: Workflow },
  { id: 3, label: "Generate", icon: Rocket },
]

export function PhaseIndicator() {
  const { currentPhase, setCurrentPhase, tables } = useGeneratorStore()

  const canNavigateTo = (phaseId: number) => {
    return true
  }

  return (
    <div className="border-b border-border bg-card/30 backdrop-blur-sm">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-center py-4">
          <div className="flex items-center gap-2 sm:gap-4">
            {phases.map((phase, index) => {
              const isActive = currentPhase === phase.id
              const isCompleted = currentPhase > phase.id
              const canClick = canNavigateTo(phase.id)

              return (
                <div key={phase.id} className="flex items-center">
                  <button
                    onClick={() => canClick && setCurrentPhase(phase.id)}
                    disabled={!canClick}
                    className={`flex items-center gap-2 px-3 py-2 sm:px-4 sm:py-2.5 rounded-xl transition-all ${isActive
                        ? "bg-gradient-to-r from-primary to-accent text-primary-foreground glow-sm"
                        : isCompleted
                          ? "bg-primary/20 text-primary"
                          : canClick
                            ? "glass text-muted-foreground hover:text-foreground"
                            : "glass text-muted-foreground/50 cursor-not-allowed"
                      }`}
                  >
                    <div className="relative">
                      {isCompleted ? (
                        <motion.div initial={{ scale: 0 }} animate={{ scale: 1 }} className="w-5 h-5">
                          <Check className="w-5 h-5" />
                        </motion.div>
                      ) : (
                        <phase.icon className="w-5 h-5" />
                      )}
                    </div>
                    <span className="hidden sm:inline text-sm font-medium">{phase.label}</span>
                    <span className="sm:hidden text-sm font-medium">{phase.id}</span>
                  </button>

                  {index < phases.length - 1 && (
                    <div
                      className={`w-8 sm:w-12 h-0.5 mx-1 sm:mx-2 rounded-full ${currentPhase > phase.id ? "bg-gradient-to-r from-primary to-accent" : "bg-border"
                        }`}
                    />
                  )}
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </div>
  )
}
