"use client"

import { useState, useEffect } from "react"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable"
import { useGeneratorStore, type FilePreview } from "@/lib/store"
import Editor from "@monaco-editor/react"
import {
    Folder,
    FileCode,
    FileJson,
    FileText,
    ChevronRight,
    ChevronDown,
    Download,
    Copy,
    X,
    Loader2,
    File as FileIcon,
} from "lucide-react"
import { cn } from "@/lib/utils"
import { toast } from "sonner"
import { motion, AnimatePresence } from "framer-motion"

interface CodePreviewModalProps {
    isOpen: boolean
    onClose: () => void
    onDownload: () => void
    isDownloading: boolean
}

// Helper to build file tree structure
interface TreeNode {
    name: string
    path: string
    type: "file" | "folder"
    children?: TreeNode[]
    file?: FilePreview
}

function buildFileTree(files: FilePreview[]): TreeNode {
    const root: TreeNode = { name: "root", path: "", type: "folder", children: [] }

    files.forEach((file) => {
        const parts = file.path.split("/")
        let current = root

        parts.forEach((part, index) => {
            const isFile = index === parts.length - 1
            const existingChild = current.children?.find((child) => child.name === part)

            if (existingChild) {
                current = existingChild
            } else {
                const newNode: TreeNode = {
                    name: part,
                    path: parts.slice(0, index + 1).join("/"),
                    type: isFile ? "file" : "folder",
                    children: isFile ? undefined : [],
                    file: isFile ? file : undefined,
                }
                current.children?.push(newNode)
                // Sort: folders first, then files
                current.children?.sort((a, b) => {
                    if (a.type === b.type) return a.name.localeCompare(b.name)
                    return a.type === "folder" ? -1 : 1
                })
                current = newNode
            }
        })
    })

    return root
}

// File Tree Item Component
const FileTreeItem = ({
    node,
    level,
    selectedPath,
    onSelect,
}: {
    node: TreeNode
    level: number
    selectedPath: string | null
    onSelect: (file: FilePreview) => void
}) => {
    const [isOpen, setIsOpen] = useState(true)
    const isSelected = node.type === "file" && node.file?.path === selectedPath

    const getIcon = () => {
        if (node.type === "folder") return <Folder className="w-4 h-4 text-blue-400" />
        if (node.name.endsWith(".java")) return <FileCode className="w-4 h-4 text-orange-400" />
        if (node.name.endsWith(".xml")) return <FileCode className="w-4 h-4 text-red-400" />
        if (node.name.endsWith(".json")) return <FileJson className="w-4 h-4 text-yellow-400" />
        if (node.name.endsWith(".properties")) return <FileText className="w-4 h-4 text-green-400" />
        return <FileIcon className="w-4 h-4 text-gray-400" />
    }

    if (node.type === "folder") {
        return (
            <div>
                <div
                    className="flex items-center py-1 px-2 hover:bg-accent/50 cursor-pointer select-none text-sm"
                    style={{ paddingLeft: `${level * 12 + 4}px` }}
                    onClick={() => setIsOpen(!isOpen)}
                >
                    <span className="mr-1 opacity-70">
                        {isOpen ? <ChevronDown className="w-3 h-3" /> : <ChevronRight className="w-3 h-3" />}
                    </span>
                    <span className="mr-2">{getIcon()}</span>
                    <span className="truncate">{node.name}</span>
                </div>
                {isOpen && node.children?.map((child) => (
                    <FileTreeItem
                        key={child.path}
                        node={child}
                        level={level + 1}
                        selectedPath={selectedPath}
                        onSelect={onSelect}
                    />
                ))}
            </div>
        )
    }

    return (
        <div
            className={cn(
                "flex items-center py-1 px-2 cursor-pointer text-sm border-l-2 border-transparent",
                isSelected ? "bg-accent border-primary" : "hover:bg-accent/50"
            )}
            style={{ paddingLeft: `${level * 12 + 16}px` }}
            onClick={() => node.file && onSelect(node.file)}
        >
            <span className="mr-2">{getIcon()}</span>
            <span className="truncate">{node.name}</span>
        </div>
    )
}

export function CodePreviewModal({
    isOpen,
    onClose,
    onDownload,
    isDownloading,
}: CodePreviewModalProps) {
    const { previewFiles, updatePreviewFile, projectConfig } = useGeneratorStore();
    const [selectedFile, setSelectedFile] = useState<FilePreview | null>(null);
    const [fileTree, setFileTree] = useState<TreeNode | null>(null);

    useEffect(() => {
        if (previewFiles.length > 0) {
            const tree = buildFileTree(previewFiles);
            setFileTree(tree);

            if (!selectedFile) {
                const findFirstFile = (node: TreeNode): FilePreview | null => {
                    if (node.type === "file" && node.file) return node.file;
                    if (node.children) {
                        for (const child of node.children) {
                            const found = findFirstFile(child);
                            if (found) return found;
                        }
                    }
                    return null;
                };

                const first = findFirstFile(tree);
                if (first) setSelectedFile(first);
            }
        }
    }, [previewFiles]);

    const handleEditorChange = (value?: string) => {
        if (selectedFile && value !== undefined) {
            updatePreviewFile(selectedFile.path, value);
        }
    };

    const handleCopy = () => {
        if (selectedFile) {
            navigator.clipboard.writeText(selectedFile.content);
            toast.success("Copied to clipboard");
        }
    };

    const getLanguage = (path: string) => {
        if (path.endsWith(".java")) return "java";
        if (path.endsWith(".xml")) return "xml";
        if (path.endsWith(".json")) return "json";
        if (path.endsWith(".js")) return "javascript";
        if (path.endsWith(".ts")) return "typescript";
        if (path.endsWith(".css")) return "css";
        if (path.endsWith(".html")) return "html";
        if (path.endsWith(".sql")) return "sql";
        if (path.endsWith(".md")) return "markdown";
        if (path.endsWith(".properties")) return "ini";
        if (path.endsWith(".yml") || path.endsWith(".yaml")) return "yaml";
        return "plaintext";
    };

    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
            <DialogContent
                showCloseButton={false}
                className="
                    !fixed !left-0 !top-0
                    !translate-x-0 !translate-y-0
                    !w-screen !h-screen
                    !max-w-none !max-h-none
                    !m-0 !p-0 
                    !rounded-none
                    !border-0
                    bg-[#1e1e1e]
                    overflow-hidden flex flex-col
                    !inset-0
                "
            >

                {/* Header */}
                <div className="flex items-center justify-between px-4 py-3 border-b border-border/10 bg-[#252526] shrink-0">
                    <div className="flex items-center gap-3">
                        <DialogTitle className="text-foreground font-normal">
                            {projectConfig.artifactId}.zip
                        </DialogTitle>

                        {selectedFile && (
                            <span className="text-xs text-muted-foreground bg-secondary/20 px-2 py-0.5 rounded">
                                {selectedFile.path}
                            </span>
                        )}
                    </div>

                    <div className="flex items-center gap-2">
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={handleCopy}
                            className="h-8 bg-transparent border-border/20 hover:bg-white/5"
                        >
                            <Copy className="w-4 h-4 mr-2" />
                            Copy
                        </Button>

                        <Button
                            size="sm"
                            onClick={onDownload}
                            disabled={isDownloading}
                            className="h-8 bg-primary hover:bg-primary/90"
                        >
                            {isDownloading ? (
                                <Loader2 className="w-4 h-4 animate-spin mr-2" />
                            ) : (
                                <Download className="w-4 h-4 mr-2" />
                            )}
                            Download
                        </Button>

                        <Button
                            variant="ghost"
                            size="icon"
                            onClick={onClose}
                            className="h-8 w-8 hover:bg-white/10 ml-2"
                        >
                            <X className="w-4 h-4" />
                        </Button>
                    </div>
                </div>

                {/* Body */}
                <div className="flex-1 min-h-0 overflow-hidden">
                    <ResizablePanelGroup direction="horizontal">
                        {/* File Tree */}
                        <ResizablePanel
                            defaultSize={20}
                            minSize={12}
                            maxSize={40}
                            className="bg-[#252526]"
                        >
                            <ScrollArea className="h-full">
                                <div className="p-2">
                                    {fileTree?.children?.map((child) => (
                                        <FileTreeItem
                                            key={child.path}
                                            node={child}
                                            level={0}
                                            selectedPath={selectedFile?.path ?? null}
                                            onSelect={setSelectedFile}
                                        />
                                    ))}
                                </div>
                            </ScrollArea>
                        </ResizablePanel>

                        <ResizableHandle className="bg-border/10" />

                        {/* Editor */}
                        <ResizablePanel defaultSize={80}>
                            <div className="h-full min-h-0">
                                {selectedFile ? (
                                    <Editor
                                        height="100%"
                                        language={getLanguage(selectedFile.path)}
                                        value={selectedFile.content}
                                        theme="vs-dark"
                                        onChange={handleEditorChange}
                                        options={{
                                            minimap: { enabled: false },
                                            fontSize: 14,
                                            lineNumbers: "on",
                                            scrollBeyondLastLine: false,
                                            automaticLayout: true,
                                            padding: { top: 16, bottom: 16 },
                                            fontFamily:
                                                "'JetBrains Mono', 'Fira Code', Consolas, monospace",
                                        }}
                                    />
                                ) : (
                                    <div className="flex items-center justify-center h-full text-muted-foreground">
                                        Select a file to view
                                    </div>
                                )}
                            </div>
                        </ResizablePanel>
                    </ResizablePanelGroup>
                </div>

                {/* Footer */}
                <div className="px-4 py-1 bg-[#007acc] text-white text-xs flex items-center justify-between shrink-0">
                    <div className="flex gap-4">
                        <span>
                            {selectedFile ? getLanguage(selectedFile.path).toUpperCase() : ""}
                        </span>
                        <span>UTF-8</span>
                    </div>
                    <div className="flex gap-2">
                        <span>CTRL + S to Download</span>
                        <span>ESC to Close</span>
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    );
}
