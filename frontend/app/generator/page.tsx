"use client"

import { motion, AnimatePresence } from "framer-motion"
import { Navbar } from "@/components/navbar"
import { useGeneratorStore } from "@/lib/store"
import { PhaseIndicator } from "@/components/generator/phase-indicator"
import { SqlParserPhase } from "@/components/generator/sql-parser-phase"
import { SchemaEditorPhase } from "@/components/generator/schema-editor-phase"
import { ProjectConfigPhase } from "@/components/generator/project-config-phase"
import { SecurityPhase } from "@/components/generator/security-phase"

export default function GeneratorPage() {
  const { currentPhase } = useGeneratorStore()

  return (
    <main className="min-h-screen flex flex-col">
      <Navbar />

      <div className="flex-1 pt-20">
        {/* Phase Indicator */}
        <PhaseIndicator />

        {/* Phase Content */}
        <AnimatePresence mode="wait">
          {currentPhase === 1 && (
            <motion.div
              key="phase-1"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.3 }}
            >
              <SqlParserPhase />
            </motion.div>
          )}

          {currentPhase === 2 && (
            <motion.div
              key="phase-2"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.3 }}
              className="flex-1"
            >
              <SchemaEditorPhase />
            </motion.div>
          )}

          {currentPhase === 3 && (
            <motion.div
              key="phase-3"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.3 }}
            >
              <SecurityPhase />
            </motion.div>
          )}

          {currentPhase === 4 && (
            <motion.div
              key="phase-4"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.3 }}
            >
              <ProjectConfigPhase />
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </main>
  )
}
