package com.firas.generator.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firas.generator.mcp.McpToolProvider;
import com.firas.generator.model.FilePreview;
import com.firas.generator.model.ProjectRequest;
import com.firas.generator.stack.StackProvider;
import com.firas.generator.stack.StackProviderFactory;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP tool that generates a project preview with all file contents.
 */
@Component
public class PreviewProjectTool implements McpToolProvider {

    private static final Logger log = LoggerFactory.getLogger(PreviewProjectTool.class);

    private final StackProviderFactory stackProviderFactory;
    private final ObjectMapper objectMapper;

    public PreviewProjectTool(StackProviderFactory stackProviderFactory, ObjectMapper objectMapper) {
        this.stackProviderFactory = stackProviderFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public McpServerFeatures.SyncToolSpecification getToolSpecification() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "projectRequest", Map.of("type", "object",
                                "description", "Full project request. Key fields: stackType (SPRING/NODE/NEST/FASTAPI), " +
                                        "name, description, packageName, tables (from parse_sql), " +
                                        "dependencies (list of {id, name, description, groupId, artifactId, scope, isStarter}), " +
                                        "includeEntity/includeRepository/includeService/includeController (booleans), " +
                                        "springConfig ({groupId, artifactId, javaVersion, bootVersion, buildTool, packaging}), " +
                                        "securityConfig (optional, use configure_security to generate)")
                ),
                List.of("projectRequest"),
                null, null, null
        );

        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("preview_project")
                .description("Generate a project preview showing all files that would be created. " +
                        "Returns an array of {path, content, language} for each generated file. " +
                        "Use parse_sql first for tables, list_dependencies for dependency IDs.")
                .inputSchema(inputSchema)
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            try {
                Object requestObj = arguments.get("projectRequest");
                ProjectRequest request = objectMapper.convertValue(requestObj, ProjectRequest.class);

                StackProvider provider = stackProviderFactory.getProvider(request.getStackType());
                List<FilePreview> files = provider.generateProject(request);

                List<Map<String, String>> result = files.stream()
                        .map(f -> Map.of(
                                "path", f.getPath(),
                                "language", f.getLanguage(),
                                "content", f.getContent()
                        ))
                        .toList();

                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), false);
            } catch (Exception e) {
                log.error("Failed to preview project", e);
                return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("Error previewing project: " + e.getMessage())), true);
            }
        });
    }
}
