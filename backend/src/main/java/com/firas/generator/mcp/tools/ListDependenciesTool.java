package com.firas.generator.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firas.generator.mcp.McpToolProvider;
import com.firas.generator.model.DependencyGroup;
import com.firas.generator.stack.DependencyProvider;
import com.firas.generator.stack.StackProviderFactory;
import com.firas.generator.stack.StackType;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP tool that lists available dependency groups for a given technology stack.
 */
@Component
public class ListDependenciesTool implements McpToolProvider {

    private static final Logger log = LoggerFactory.getLogger(ListDependenciesTool.class);

    private final StackProviderFactory stackProviderFactory;
    private final ObjectMapper objectMapper;

    public ListDependenciesTool(StackProviderFactory stackProviderFactory, ObjectMapper objectMapper) {
        this.stackProviderFactory = stackProviderFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public McpServerFeatures.SyncToolSpecification getToolSpecification() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "stackType", Map.of("type", "string",
                                "enum", List.of("SPRING", "NODE", "NEST", "FASTAPI"),
                                "description", "Technology stack to list dependencies for (default: SPRING)")
                ),
                List.of(),
                null, null, null
        );

        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("list_dependencies")
                .description("List available dependency groups and their dependencies for a technology stack. " +
                        "Each group contains dependencies with id, name, description, and Maven coordinates. " +
                        "Use the dependency IDs when building a project request.")
                .inputSchema(inputSchema)
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            try {
                String stackStr = arguments.containsKey("stackType")
                        ? (String) arguments.get("stackType")
                        : "SPRING";
                StackType stackType = StackType.valueOf(stackStr.toUpperCase());

                DependencyProvider depProvider = stackProviderFactory.getProvider(stackType)
                        .getDependencyProvider();
                List<DependencyGroup> groups = depProvider.getAllGroups();

                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(groups);
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), false);
            } catch (Exception e) {
                log.error("Failed to list dependencies", e);
                return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("Error listing dependencies: " + e.getMessage())), true);
            }
        });
    }
}
