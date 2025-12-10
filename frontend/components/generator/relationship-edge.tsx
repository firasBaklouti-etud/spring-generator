import {
    BaseEdge,
    EdgeLabelRenderer,
    getSmoothStepPath,
    type EdgeProps,
    getBezierPath,
} from "@xyflow/react"
import React from "react"

export function RelationshipEdge({
    id,
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition,
    targetPosition,
    style = {},
    markerEnd,
    data,
}: EdgeProps) {
    const [edgePath, labelX, labelY] = getSmoothStepPath({
        sourceX,
        sourceY,
        sourcePosition,
        targetX,
        targetY,
        targetPosition,
    })

    // Determine labels based on data or default to 1..N
    const sourceLabel = (data?.sourceLabel as string) || "1"
    const targetLabel = (data?.targetLabel as string) || "N"

    return (
        <>
            <BaseEdge path={edgePath} markerEnd={markerEnd} style={style} />
            <EdgeLabelRenderer>
                {/* Source Label (near start) */}
                <div
                    style={{
                        position: 'absolute',
                        transform: `translate(-50%, -50%) translate(${sourceX}px,${sourceY}px)`,
                        pointerEvents: 'none',
                    }}
                    className="nodrag nopan"
                >
                    <div className="bg-card text-foreground text-[10px] font-bold px-1.5 py-0.5 rounded-full border border-border shadow-sm">
                        {sourceLabel}
                    </div>
                </div>

                {/* Target Label (near end) */}
                <div
                    style={{
                        position: 'absolute',
                        transform: `translate(-50%, -50%) translate(${targetX}px,${targetY}px)`,
                        pointerEvents: 'none',
                    }}
                    className="nodrag nopan"
                >
                    <div className="bg-card text-foreground text-[10px] font-bold px-1.5 py-0.5 rounded-full border border-border shadow-sm">
                        {targetLabel}
                    </div>
                </div>
            </EdgeLabelRenderer>
        </>
    )
}
