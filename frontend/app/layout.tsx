import type React from "react"
import type { Metadata } from "next"
import { Inter, JetBrains_Mono } from "next/font/google"
import { Analytics } from "@vercel/analytics/next"
import { Toaster } from "sonner"
import "./globals.css"

const inter = Inter({ subsets: ["latin"], variable: "--font-inter" })
const jetbrainsMono = JetBrains_Mono({ subsets: ["latin"], variable: "--font-mono" })

export const metadata: Metadata = {
  title: "Spring Generator - Build Spring Boot Projects in Seconds",
  description:
    "Generate production-ready Spring Boot projects from SQL schemas. Visual database editor, AI-powered schema generation, and instant project downloads.",
  generator: "Spring Generator",
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en" className="dark">
      <body className={`${inter.variable} ${jetbrainsMono.variable} font-sans antialiased`}>
        {children}
        <Toaster
          theme="dark"
          position="top-right"
          toastOptions={{
            style: {
              background: "oklch(0.12 0.01 260)",
              border: "1px solid oklch(0.25 0.02 260)",
              color: "oklch(0.95 0 0)",
            },
          }}
        />
        <Analytics />
      </body>
    </html>
  )
}
