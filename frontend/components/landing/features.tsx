"use client"

import { motion } from "framer-motion"
import { Database, Workflow, Download, Sparkles, Layers, Shield } from "lucide-react"

const features = [
  {
    icon: Database,
    title: "SQL Parsing",
    description: "Paste your MySQL or PostgreSQL CREATE TABLE statements and watch them transform into structured data models.",
  },
  {
    icon: Workflow,
    title: "Visual Schema Editor",
    description: "Drag, drop, and connect tables on an infinite canvas. Edit relationships (1..N) with intuitive controls.",
  },
  {
    icon: Sparkles,
    title: "AI Generation",
    description: "Describe your database in plain English and let AI generate the complete schema for you.",
  },
  {
    icon: Layers,
    title: "Multi-Stack Support",
    description: "Generate backends for Spring Boot, Node.js, NestJS, and FastAPI with proper conventions.",
  },
  {
    icon: Shield,
    title: "Best Practices",
    description: "Code follows Spring Boot conventions, includes validation, and implements security patterns.",
  },
  {
    icon: Download,
    title: "Instant Download",
    description: "Get a complete, runnable Maven/Gradle project as a ZIP file ready to import.",
  },
]

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
    },
  },
}

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.5,
    },
  },
}

export function Features() {
  return (
    <section className="py-24 lg:py-32 relative">
      {/* Background */}
      <div className="absolute inset-0 bg-gradient-to-b from-transparent via-primary/5 to-transparent" />

      <div className="container mx-auto px-4 sm:px-6 lg:px-8 relative">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5 }}
          className="text-center max-w-3xl mx-auto mb-16"
        >
          <span className="text-primary font-medium mb-4 block">Features</span>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold tracking-tight mb-4 text-balance">
            Everything you need to <span className="gradient-text">ship faster</span>
          </h2>
          <p className="text-lg text-muted-foreground text-pretty">
            From SQL to production-ready Spring Boot in three simple steps. No boilerplate, no configuration headaches.
          </p>
        </motion.div>

        {/* Feature Grid */}
        <motion.div
          variants={containerVariants}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, margin: "-100px" }}
          className="grid md:grid-cols-2 lg:grid-cols-3 gap-6"
        >
          {features.map((feature, i) => (
            <motion.div key={i} variants={itemVariants} className="group relative">
              <div className="glass rounded-2xl p-6 h-full transition-all duration-300 hover:bg-card/60 gradient-border">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary/20 to-accent/20 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                  <feature.icon className="w-6 h-6 text-primary" />
                </div>
                <h3 className="text-lg font-semibold mb-2">{feature.title}</h3>
                <p className="text-muted-foreground text-sm">{feature.description}</p>
              </div>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  )
}
