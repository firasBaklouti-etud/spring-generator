package com.firas.generator.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;

/**
 * Interface for components that provide MCP tool specifications.
 * Each implementation exposes a single tool to AI clients via the MCP protocol.
 */
public interface McpToolProvider {
    McpServerFeatures.SyncToolSpecification getToolSpecification();
}
