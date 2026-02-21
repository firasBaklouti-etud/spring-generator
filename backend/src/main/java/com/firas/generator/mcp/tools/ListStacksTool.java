package com.firas.generator.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firas.generator.mcp.McpToolProvider;
import com.firas.generator.stack.StackProviderFactory;
import com.firas.generator.stack.StackType;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP tool that lists all available technology stacks.
 */
@Component
public class ListStacksTool implements McpToolProvider {

    private final StackProviderFactory stackProviderFactory;
    private final ObjectMapper objectMapper;

    public ListStacksTool(StackProviderFactory stackProviderFactory, ObjectMapper objectMapper) {
        this.stackProviderFactory = stackProviderFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public McpServerFeatures.SyncToolSpecification getToolSpecification() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object", Map.of(), List.of(), null, null, null
        );

        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("list_stacks")
                .description("List all available technology stacks for project generation. " +
                        "Returns stack types with their IDs, display names, languages, and default versions.")
                .inputSchema(inputSchema)
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            try {
                List<StackType> stacks = stackProviderFactory.getAvailableStackTypes();
                List<Map<String, String>> result = stacks.stream()
                        .map(s -> Map.of(
                                "id", s.name(),
                                "displayName", s.getDisplayName(),
                                "language", s.getLanguage(),
                                "defaultVersion", s.getDefaultVersion()
                        ))
                        .toList();

                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), false);
            } catch (Exception e) {
                return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("Error listing stacks: " + e.getMessage())), true);
            }
        });
    }
}
