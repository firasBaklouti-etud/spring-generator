package com.firas.generator.mcp;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

/**
 * Configures the MCP (Model Context Protocol) server with SSE transport.
 *
 * Exposes the project generator's capabilities as MCP tools accessible by
 * AI clients (Claude Desktop, Cursor, etc.) via Server-Sent Events.
 *
 * SSE endpoint: /sse (client connects here)
 * Message endpoint: /mcp/message (client sends requests here)
 */
@Configuration
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    @Bean
    WebMvcSseServerTransportProvider mcpSseTransport() {
        return WebMvcSseServerTransportProvider.builder()
                .messageEndpoint("/mcp/message")
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> mcpRouterFunction(WebMvcSseServerTransportProvider transport) {
        return transport.getRouterFunction();
    }

    @Bean
    McpSyncServer mcpSyncServer(WebMvcSseServerTransportProvider transport,
                                List<McpToolProvider> toolProviders) {
        List<McpServerFeatures.SyncToolSpecification> tools = toolProviders.stream()
                .map(McpToolProvider::getToolSpecification)
                .toList();

        log.info("Registering {} MCP tools", tools.size());
        tools.forEach(t -> log.info("  - {}", t.tool().name()));

        return McpServer.sync(transport)
                .serverInfo("9raya-generator", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(tools)
                .build();
    }
}
